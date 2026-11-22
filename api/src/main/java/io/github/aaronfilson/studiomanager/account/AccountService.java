package io.github.aaronfilson.studiomanager.account;

import io.github.aaronfilson.studiomanager.platform.ConflictException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;
import io.github.aaronfilson.studiomanager.platform.tenancy.McpOAuthTokenVerifier;
import io.github.aaronfilson.studiomanager.platform.tenancy.SystemActor;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts are keyed by the token's issuer and subject, and created on first sign-in. There is no
 * separate sign-up step: the identity provider already did that.
 *
 * <p>External OAuth identities (such as the MCP OAuth 2.1 authorization server) can be linked to an
 * existing account via {@code account_identity} once the caller proves live possession of a signed
 * JWT from that issuer (ADR 26).
 */
@Service
class AccountService implements AccountResolver {

  /** One external OAuth identity linked to an account. */
  record LinkedIdentity(UUID id, String issuer, String subject, Instant createdAt) {}

  private final AccountRepository accounts;
  private final JdbcClient jdbc;
  private final McpOAuthTokenVerifier mcpOAuthVerifier;
  private final Clock clock;

  AccountService(
      AccountRepository accounts,
      JdbcClient jdbc,
      McpOAuthTokenVerifier mcpOAuthVerifier,
      Clock clock) {
    this.accounts = accounts;
    this.jdbc = jdbc;
    this.mcpOAuthVerifier = mcpOAuthVerifier;
    this.clock = clock;
  }

  /**
   * Runs on every authenticated request, so the common case is a single indexed read. It writes
   * only on first sign-in or when the name or email in the token has changed.
   */
  @Override
  @Transactional
  public UUID resolve(Jwt token) {
    // Read as a string: getIssuer() parses a URL, and the system issuer is a URN
    String issuer =
        Objects.requireNonNull(token.getClaimAsString("iss"), "The token has no issuer");
    if (SystemActor.ISSUER.equals(issuer)) {
      // No identity provider issues this, but a token naming it must never become a background job
      throw new AccessDeniedException("This account can't sign in.");
    }
    String subject = Objects.requireNonNull(token.getSubject(), "The token has no subject");
    String displayName = token.getClaimAsString("name");
    String email = token.getClaimAsString("email");
    return accounts
        .findByIssuerAndSubject(issuer, subject)
        .filter(account -> account.hasProfile(displayName, email))
        .map(Account::id)
        // An upsert, not an insert, so two first requests racing each other both succeed
        .orElseGet(() ->
            jdbc.sql(
                    """
                    insert into account (id, issuer, subject, display_name, email)
                    values (:id, :issuer, :subject, :displayName, :email)
                    on conflict (issuer, subject) do update
                        set display_name = excluded.display_name,
                            email = excluded.email,
                            updated_at = now()
                    returning id
                    """)
                .param("id", Uuids.v7())
                .param("issuer", issuer)
                .param("subject", subject)
                .param("displayName", displayName)
                .param("email", email)
                .query(UUID.class)
                .single());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> findBySubject(String tokenIssuer, String subject) {
    if (tokenIssuer.isBlank() || subject.isBlank() || SystemActor.ISSUER.equals(tokenIssuer)) {
      return Optional.empty();
    }
    Optional<UUID> linked =
        jdbc.sql(
                """
                select ai.account_id
                from account_identity ai
                join account a on a.id = ai.account_id
                where ai.issuer = :issuer
                  and ai.subject = :subject
                  and a.issuer <> :systemIssuer
                """)
            .param("issuer", tokenIssuer)
            .param("subject", subject)
            .param("systemIssuer", SystemActor.ISSUER)
            .query(UUID.class)
            .optional();
    if (linked.isPresent()) {
      return linked;
    }
    return accounts
        .findByIssuerAndSubject(tokenIssuer, subject)
        .filter(AccountService::isPerson)
        .map(Account::id);
  }

  /** Lists external identities linked to {@code accountId}, oldest first. */
  @Transactional(readOnly = true)
  List<LinkedIdentity> listIdentities(UUID accountId) {
    return jdbc.sql(
            """
            select id, issuer, subject, created_at
            from account_identity
            where account_id = :accountId
            order by created_at asc, id asc
            """)
        .param("accountId", accountId)
        .query((rs, row) ->
            new LinkedIdentity(
                rs.getObject("id", UUID.class),
                rs.getString("issuer"),
                rs.getString("subject"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()))
        .list();
  }

  /**
   * Verifies a live MCP OAuth JWT and links its {@code (iss, sub)} to {@code accountId}. Idempotent
   * when already linked to the same account; refuses with 409 if already owned by another account.
   */
  @Transactional
  LinkedIdentity linkMcpIdentity(UUID accountId, String rawToken) {
    McpOAuthTokenVerifier.VerifiedIdentity verified = mcpOAuthVerifier.verify(rawToken);
    Optional<Account> primaryOwner =
        accounts.findByIssuerAndSubject(verified.issuer(), verified.subject());
    if (primaryOwner.isPresent() && !primaryOwner.get().id().equals(accountId)) {
      throw new ConflictException(
          "That MCP identity already belongs to a different Studio Manager account.");
    }
    Optional<LinkedIdentityRow> existing =
        jdbc.sql(
                """
                select id, account_id, issuer, subject, created_at
                from account_identity
                where issuer = :issuer and subject = :subject
                """)
            .param("issuer", verified.issuer())
            .param("subject", verified.subject())
            .query((rs, row) ->
                new LinkedIdentityRow(
                    rs.getObject("id", UUID.class),
                    rs.getObject("account_id", UUID.class),
                    rs.getString("issuer"),
                    rs.getString("subject"),
                    rs.getObject("created_at", OffsetDateTime.class).toInstant()))
            .optional();
    if (existing.isPresent()) {
      LinkedIdentityRow found = existing.get();
      if (!found.accountId().equals(accountId)) {
        throw new ConflictException(
            "That MCP identity is already linked to a different Studio Manager account.");
      }
      return new LinkedIdentity(found.id(), found.issuer(), found.subject(), found.createdAt());
    }
    UUID id = Uuids.v7();
    Instant now = clock.instant();
    jdbc.sql(
            """
            insert into account_identity (id, account_id, issuer, subject, created_at)
            values (:id, :accountId, :issuer, :subject, :createdAt)
            """)
        .param("id", id)
        .param("accountId", accountId)
        .param("issuer", verified.issuer())
        .param("subject", verified.subject())
        .param("createdAt", now.atOffset(ZoneOffset.UTC))
        .update();
    return new LinkedIdentity(id, verified.issuer(), verified.subject(), now);
  }

  /** Removes a linked identity belonging to {@code accountId}. */
  @Transactional
  void unlinkIdentity(UUID accountId, UUID identityId) {
    int deleted =
        jdbc.sql("delete from account_identity where id = :id and account_id = :accountId")
            .param("id", identityId)
            .param("accountId", accountId)
            .update();
    if (deleted == 0) {
      throw new NotFoundException("No such linked identity.");
    }
  }

  private record LinkedIdentityRow(
      UUID id, UUID accountId, String issuer, String subject, Instant createdAt) {}

  private static boolean isPerson(Account account) {
    return !SystemActor.ISSUER.equals(account.issuer());
  }

  @Transactional(readOnly = true)
  Account get(UUID accountId) {
    return accounts
        .findById(accountId)
        .orElseThrow(() -> new NotFoundException("No such account."));
  }
}
