package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.jspecify.annotations.Nullable;

/** A studio: the tenant that every studio-owned row belongs to. */
@Entity
@Table(name = "studio")
class Studio {

    @Id
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private UnitSystem unitSystem;

    private String timeZone;

    // ISO 3166-1 alpha-2
    private String country;

    // ISO 4217
    private String currency;

    private Instant createdAt;

    // Null until first saved, which is also how Spring Data tells new from existing with an assigned ID
    @Version
    private @Nullable Long version;

    protected Studio() {
    }

    Studio(UUID id, String name, UnitSystem unitSystem, ZoneId timeZone, String country, Currency currency,
            Instant createdAt) {
        this.id = id;
        this.name = name;
        this.unitSystem = unitSystem;
        this.timeZone = timeZone.getId();
        this.country = country;
        this.currency = currency.getCurrencyCode();
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    UnitSystem unitSystem() {
        return unitSystem;
    }

    ZoneId timeZone() {
        return ZoneId.of(timeZone);
    }

    String country() {
        return country;
    }

    Currency currency() {
        return Currency.getInstance(currency);
    }

    Instant createdAt() {
        return createdAt;
    }

}
