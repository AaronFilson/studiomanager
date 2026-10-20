package io.github.aaronfilson.studiomanager.account;

import java.util.Objects;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.Uuids;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountResolver;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts are keyed by the token's issuer and subject, and created on first sign-in. There is
 * no separate sign-up step: the identity provider already did that.
 */
@Service
class AccountService implements AccountResolver {

    private final AccountRepository accounts;
    private final JdbcClient jdbc;

    AccountService(AccountRepository accounts, JdbcClient jdbc) {
        this.accounts = accounts;
        this.jdbc = jdbc;
    }

    /**
     * Runs on every authenticated request, so the common case is a single indexed read. It
     * writes only on first sign-in or when the name or email in the token has changed.
     */
    @Override
    @Transactional
    public UUID resolve(Jwt token) {
        String issuer = Objects.requireNonNull(token.getIssuer(), "The token has no issuer").toString();
        String subject = Objects.requireNonNull(token.getSubject(), "The token has no subject");
        String displayName = token.getClaimAsString("name");
        String email = token.getClaimAsString("email");
        return accounts.findByIssuerAndSubject(issuer, subject)
                .filter(account -> account.hasProfile(displayName, email))
                .map(Account::id)
                // An upsert, not an insert, so two first requests racing each other both succeed
                .orElseGet(() -> jdbc.sql("""
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

    @Transactional(readOnly = true)
    Account get(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new NotFoundException("No such account."));
    }

}
