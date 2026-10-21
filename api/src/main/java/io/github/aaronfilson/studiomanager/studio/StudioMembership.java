package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

/** One person's membership, and role, in one studio. */
@Entity
@Table(name = "studio_membership")
class StudioMembership {

    @Id
    private UUID id;

    private UUID studioId;

    private UUID accountId;

    @Enumerated(EnumType.STRING)
    private StudioRole role;

    private Instant createdAt;

    @Version
    private @Nullable Long version;

    protected StudioMembership() {
    }

    StudioMembership(UUID id, UUID studioId, UUID accountId, StudioRole role, Instant createdAt) {
        this.id = id;
        this.studioId = studioId;
        this.accountId = accountId;
        this.role = role;
        this.createdAt = createdAt;
    }

}
