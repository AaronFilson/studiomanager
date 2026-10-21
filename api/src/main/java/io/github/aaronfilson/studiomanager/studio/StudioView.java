package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

/** A studio as seen by one of its members. */
public record StudioView(UUID id, String name, UnitSystem unitSystem, ZoneId timeZone, String country,
        Currency currency, StudioRole myRole, int memberCount, Instant createdAt) {

    static StudioView of(Studio studio, StudioRole myRole, int memberCount) {
        return new StudioView(studio.id(), studio.name(), studio.unitSystem(), studio.timeZone(), studio.country(),
                studio.currency(), myRole, memberCount, studio.createdAt());
    }

}
