package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;

/**
 * Someone accepted an invite into a studio. Published after the membership exists; also sent
 * when an existing member accepts another invite, so listeners must be idempotent.
 */
public record MemberJoined(UUID studioId, UUID accountId) {
}
