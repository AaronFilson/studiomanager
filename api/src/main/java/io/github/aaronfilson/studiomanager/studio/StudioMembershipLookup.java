package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.MemberRole;
import io.github.aaronfilson.studiomanager.platform.tenancy.MembershipLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class StudioMembershipLookup implements MembershipLookup {

  private final StudioMembershipRepository memberships;

  StudioMembershipLookup(StudioMembershipRepository memberships) {
    this.memberships = memberships;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<MemberRole> roleOf(UUID accountId, UUID studioId) {
    return memberships.findMemberRole(studioId, accountId);
  }
}
