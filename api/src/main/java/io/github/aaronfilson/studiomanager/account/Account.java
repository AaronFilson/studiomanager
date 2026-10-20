package io.github.aaronfilson.studiomanager.account;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

/**
 * A person, as known to the identity provider. Global rather than per-studio, because one
 * person can belong to several studios. Written by {@link AccountService}'s upsert.
 */
@Entity
@Table(name = "account")
class Account {

    @Id
    private UUID id;

    private String issuer;

    private String subject;

    private @Nullable String displayName;

    private @Nullable String email;

    private Instant createdAt;

    private Instant updatedAt;

    protected Account() {
    }

    UUID id() {
        return id;
    }

    @Nullable String displayName() {
        return displayName;
    }

    @Nullable String email() {
        return email;
    }

    boolean hasProfile(@Nullable String displayName, @Nullable String email) {
        return Objects.equals(this.displayName, displayName) && Objects.equals(this.email, email);
    }

}
