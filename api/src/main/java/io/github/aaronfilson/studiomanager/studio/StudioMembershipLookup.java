package io.github.aaronfilson.studiomanager.studio;

import java.util.Optional;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.platform.tenancy.MembershipLookup;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;

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
    public Optional<StudioRole> roleOf(UUID accountId, UUID studioId) {
        return memberships.findRole(studioId, accountId);
    }

}
