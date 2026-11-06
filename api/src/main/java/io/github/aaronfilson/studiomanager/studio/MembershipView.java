package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.UUID;

/** A studio someone belongs to, and their role there. */
public record MembershipView(UUID studioId, String studioName, StudioRole role) {}
