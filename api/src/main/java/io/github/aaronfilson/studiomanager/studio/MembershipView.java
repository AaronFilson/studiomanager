package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

/** A studio someone belongs to, and their role there. */
public record MembershipView(UUID studioId, String studioName, StudioRole role) {
}
