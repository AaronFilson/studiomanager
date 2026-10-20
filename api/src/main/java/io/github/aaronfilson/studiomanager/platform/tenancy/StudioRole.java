package io.github.aaronfilson.studiomanager.platform.tenancy;

/** A person's role within one studio. Each role can do everything the roles below it can. */
public enum StudioRole {

    MEMBER(1),
    STAFF(2),
    OWNER(3);

    private final int rank;

    StudioRole(int rank) {
        this.rank = rank;
    }

    /** Whether this role grants at least what {@code minimum} does. */
    public boolean includes(StudioRole minimum) {
        return rank >= minimum.rank;
    }

}
