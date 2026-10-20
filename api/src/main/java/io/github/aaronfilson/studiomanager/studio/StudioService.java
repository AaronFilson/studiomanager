package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/** Studios and membership in them. The studio module's API to the rest of the application. */
@Service
public class StudioService {

    private static final Set<String> ISO_COUNTRIES = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

    private final StudioRepository studios;
    private final StudioMembershipRepository memberships;
    private final TransactionOperations transactions;

    StudioService(StudioRepository studios, StudioMembershipRepository memberships,
            TransactionOperations transactions) {
        this.studios = studios;
        this.memberships = memberships;
        this.transactions = transactions;
    }

    /**
     * Creates a studio with the calling account as its owner. Without a {@code currency}, the
     * studio uses its country's.
     */
    public StudioView create(String name, UnitSystem unitSystem, String timeZone, String country,
            @Nullable String currency) {
        TenantContext caller = TenantContext.require();
        String trimmedName = name.strip();
        if (trimmedName.isEmpty()) {
            throw new InvalidInputException("A studio needs a name.");
        }
        ZoneId zone = parseTimeZone(timeZone);
        String countryCode = parseCountry(country);
        Currency studioCurrency = currency == null ? currencyOf(countryCode) : parseCurrency(currency);
        UUID studioId = Uuids.v7();
        // Bind the new studio before the transaction begins, so row-level security sees these
        // inserts as happening inside the studio they create.
        return TenantContext.callAs(caller.inStudio(studioId, StudioRole.OWNER), () -> transactions.execute(tx -> {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            Studio studio = studios.save(new Studio(studioId, trimmedName, unitSystem, zone, countryCode,
                    studioCurrency, now));
            memberships.save(new StudioMembership(Uuids.v7(), studioId, caller.accountId(), StudioRole.OWNER, now));
            return StudioView.of(studio, StudioRole.OWNER, 1);
        }));
    }

    /** The studio the current request is bound to. Membership is checked before this runs. */
    @Transactional(readOnly = true)
    public StudioView get(UUID studioId) {
        TenantContext context = TenantContext.require();
        StudioRole role = context.role();
        if (role == null || !studioId.equals(context.studioId())) {
            throw new IllegalStateException("The request is not bound to studio " + studioId);
        }
        Studio studio = studios.findById(studioId).orElseThrow(() -> new NotFoundException("No such studio."));
        return StudioView.of(studio, role, memberships.countByStudioId(studioId));
    }

    @Transactional(readOnly = true)
    public List<MembershipView> membershipsOf(UUID accountId) {
        return memberships.findMembershipsOf(accountId);
    }

    private static ZoneId parseTimeZone(String timeZone) {
        // Region IDs only: "America/Chicago" yes, "+05:00" or "EST" no
        if (!ZoneId.getAvailableZoneIds().contains(timeZone)) {
            throw new InvalidInputException("Unknown time zone '%s'. Use an IANA name such as America/Chicago."
                    .formatted(timeZone));
        }
        return ZoneId.of(timeZone);
    }

    private static String parseCountry(String country) {
        if (!ISO_COUNTRIES.contains(country)) {
            throw new InvalidInputException("Unknown country '%s'. Use an ISO 3166-1 alpha-2 code such as US or GB."
                    .formatted(country));
        }
        return country;
    }

    private static Currency parseCurrency(String currency) {
        try {
            return Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Unknown currency '%s'. Use an ISO 4217 code such as USD or GBP."
                    .formatted(currency));
        }
    }

    private static Currency currencyOf(String country) {
        try {
            // Null for places with no currency of their own, such as Antarctica
            Currency currency = Currency.getInstance(Locale.of("", country));
            if (currency != null) {
                return currency;
            }
        } catch (IllegalArgumentException e) {
            // Fall through: the JDK knows the country but not its currency
        }
        throw new InvalidInputException("Country '%s' has no default currency, so name one.".formatted(country));
    }

}
