package io.github.aaronfilson.studiomanager.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.importer.ImportModel.ConsignmentRow;
import io.github.aaronfilson.studiomanager.importer.ImportModel.ImportResult;
import io.github.aaronfilson.studiomanager.importer.ImportModel.MemberRow;
import io.github.aaronfilson.studiomanager.importer.ImportModel.Outcome;
import io.github.aaronfilson.studiomanager.importer.ImportModel.RowResult;
import io.github.aaronfilson.studiomanager.ledger.OpeningBalances;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.ConsignmentItem;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.DiscountsFrom;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.Payee;
import io.github.aaronfilson.studiomanager.ledger.PayablesModel.PayeeChanges;
import io.github.aaronfilson.studiomanager.ledger.PayablesService;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.EmailInvitesResult;
import io.github.aaronfilson.studiomanager.studio.OpenInviteView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import io.github.aaronfilson.studiomanager.studio.StudioService;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Brings a studio's existing lists in from a spreadsheet: members (with what each owed), and the
 * artists' work on its shelves. The research found a generic CSV importer, covering exports from
 * Google Sheets, Forms and Airtable, to be the first thing a studio needs on day one.
 *
 * <p>Every import runs in one transaction. A dry run, or any row with an error, rolls it all back,
 * and the result says what each row did or would do. So staff check the preview, fix the file,
 * and run it for real knowing nothing was half-imported.
 */
@Service
public class ImportService {

    static final int MAX_ROWS = 1000;
    private static final int INVITE_BATCH = 200;

    private final TransactionOperations transactions;
    private final StudioMembers members;
    private final StudioService studios;
    private final OpeningBalances openingBalances;
    private final PayablesService payables;

    ImportService(TransactionOperations transactions, StudioMembers members, StudioService studios,
            OpeningBalances openingBalances, PayablesService payables) {
        this.transactions = transactions;
        this.members = members;
        this.studios = studios;
        this.openingBalances = openingBalances;
        this.payables = payables;
    }

    /**
     * Invites each new address as a member. A balance on a row is posted now for an existing
     * member, or held until a new one joins. Someone who already has an opening balance keeps it:
     * importing the same file twice changes nothing.
     */
    public ImportResult importMembers(List<MemberRow> rows, boolean dryRun) {
        requireStaff();
        requireSize(rows);
        return run(dryRun, () -> {
            String country = studios.current().country();
            Map<String, UUID> existing = members.membersByEmail();
            List<String> emails = rows.stream().map(row -> safeEmail(row.email())).filter(Objects::nonNull).toList();
            Set<String> invited = members.openlyInvited(emails);
            Set<String> seen = new HashSet<>();
            List<RowResult> results = new ArrayList<>();
            // New addresses to invite, with any balance to hold for them
            Map<String, Held> toInvite = new LinkedHashMap<>();
            for (MemberRow row : rows) {
                try {
                    String email = ImportValues.email(row.email());
                    if (email == null) {
                        throw new InvalidInputException("No email address.");
                    }
                    BigDecimal balance = ImportValues.money(row.balance());
                    LocalDate asOf = ImportValues.date(row.balanceAsOf(), country);
                    if (balance != null && balance.signum() == 0) {
                        balance = null;
                    }
                    if (balance != null) {
                        openingBalances.validate(balance, asOf);
                    }
                    if (!seen.add(email)) {
                        results.add(new RowResult(row.line(), Outcome.SKIPPED, "%s is on an earlier row.".formatted(email)));
                        continue;
                    }
                    UUID member = existing.get(email);
                    if (member != null) {
                        if (balance == null) {
                            results.add(new RowResult(row.line(), Outcome.SKIPPED, "Already a member."));
                        } else if (openingBalances.post(member, balance, asOf)) {
                            results.add(new RowResult(row.line(), Outcome.UPDATED,
                                    "Already a member; opening balance of %s posted.".formatted(balance.toPlainString())));
                        } else {
                            results.add(new RowResult(row.line(), Outcome.SKIPPED,
                                    "Already a member with an opening balance."));
                        }
                    } else if (invited.contains(email)) {
                        results.add(new RowResult(row.line(), Outcome.SKIPPED, balance == null
                                ? "Already invited."
                                : "Already invited, so the balance can't be attached. Revoke the invite and import again."));
                    } else {
                        toInvite.put(email, new Held(balance, asOf));
                        results.add(new RowResult(row.line(), Outcome.CREATED, balance == null
                                ? "Invite emailed."
                                : "Invite emailed; opening balance of %s posts when they join.".formatted(balance.toPlainString())));
                    }
                } catch (InvalidInputException e) {
                    results.add(new RowResult(row.line(), Outcome.ERROR, e.getMessage()));
                }
            }
            // A dry run never asks for emails, even ones that would roll back unsent
            if (!dryRun && results.stream().noneMatch(r -> r.outcome() == Outcome.ERROR)) {
                invite(toInvite);
            }
            return results;
        });
    }

    /**
     * Takes in each row's work for its artist. The artist is found by email, then by name, and
     * created if neither matches; a new artist takes the row's share as their terms.
     */
    public ImportResult importConsignment(List<ConsignmentRow> rows, boolean dryRun) {
        requireStaff();
        requireSize(rows);
        return run(dryRun, () -> {
            String country = studios.current().country();
            Map<String, Payee> byEmail = new HashMap<>();
            Map<String, Payee> byName = new HashMap<>();
            for (Payee payee : payables.payees()) {
                if (payee.email() != null) {
                    byEmail.putIfAbsent(payee.email().toLowerCase(Locale.ROOT), payee);
                }
                byName.putIfAbsent(payee.name().toLowerCase(Locale.ROOT), payee);
            }
            List<RowResult> results = new ArrayList<>();
            for (ConsignmentRow row : rows) {
                try {
                    String name = ImportValues.text(row.artist());
                    String email = ImportValues.email(row.artistEmail());
                    String description = ImportValues.text(row.description());
                    BigDecimal price = ImportValues.money(row.retailPrice());
                    Integer quantity = ImportValues.wholeNumber(row.quantity());
                    BigDecimal share = ImportValues.share(row.artistShare());
                    LocalDate receivedOn = ImportValues.date(row.receivedOn(), country);
                    if (description == null) {
                        throw new InvalidInputException("No description of the work.");
                    }
                    if (price == null) {
                        throw new InvalidInputException("No retail price.");
                    }
                    Payee payee = email == null ? null : byEmail.get(email);
                    if (payee == null && name != null) {
                        payee = byName.get(name.toLowerCase(Locale.ROOT));
                    }
                    boolean created = false;
                    if (payee == null) {
                        if (name == null) {
                            throw new InvalidInputException("No artist name.");
                        }
                        payee = payables.createPayee(new PayeeChanges(name, email, null, false, null, share,
                                DiscountsFrom.SHARED, null, false));
                        created = true;
                        if (email != null) {
                            byEmail.put(email, payee);
                        }
                        byName.put(name.toLowerCase(Locale.ROOT), payee);
                    }
                    ConsignmentItem item = payables.takeIn(payee.id(), description, price,
                            quantity == null ? 1 : quantity, share, receivedOn);
                    results.add(new RowResult(row.line(), Outcome.CREATED, "Took in %d × %s for %s%s.".formatted(
                            item.quantity(), item.description(), payee.name(), created ? " (a new artist)" : "")));
                } catch (InvalidInputException e) {
                    results.add(new RowResult(row.line(), Outcome.ERROR, e.getMessage()));
                }
            }
            return results;
        });
    }

    private void invite(Map<String, Held> toInvite) {
        List<String> emails = new ArrayList<>(toInvite.keySet());
        for (int start = 0; start < emails.size(); start += INVITE_BATCH) {
            List<String> batch = emails.subList(start, Math.min(emails.size(), start + INVITE_BATCH));
            EmailInvitesResult sent = members.emailInvites(String.join("\n", batch), StudioRole.MEMBER);
            for (OpenInviteView invite : sent.invited()) {
                Held held = toInvite.get(Objects.requireNonNull(invite.email()).toLowerCase(Locale.ROOT));
                if (held != null && held.amount() != null) {
                    openingBalances.holdForInvite(invite.id(), held.amount(), held.asOf());
                }
            }
        }
    }

    private ImportResult run(boolean dryRun, java.util.function.Supplier<List<RowResult>> work) {
        return transactions.execute(tx -> {
            List<RowResult> results = work.get();
            boolean commit = !dryRun && results.stream().noneMatch(r -> r.outcome() == Outcome.ERROR);
            if (!commit) {
                tx.setRollbackOnly();
            }
            return new ImportResult(commit, results);
        });
    }

    private static @Nullable String safeEmail(@Nullable String cell) {
        try {
            return ImportValues.email(cell);
        } catch (InvalidInputException e) {
            return null;
        }
    }

    private static void requireSize(List<?> rows) {
        if (rows.isEmpty() || rows.size() > MAX_ROWS) {
            throw new InvalidInputException("Import from 1 to %,d rows at once; split a longer file.".formatted(MAX_ROWS));
        }
    }

    private static void requireStaff() {
        TenantContext caller = TenantContext.require();
        if (caller.role() == null || !caller.role().includes(StudioRole.STAFF)) {
            throw new AccessDeniedException("Only staff can import.");
        }
    }

    private record Held(@Nullable BigDecimal amount, @Nullable LocalDate asOf) {
    }

}
