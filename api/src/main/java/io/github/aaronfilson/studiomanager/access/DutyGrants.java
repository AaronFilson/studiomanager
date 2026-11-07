package io.github.aaronfilson.studiomanager.access;

import io.github.aaronfilson.studiomanager.platform.tenancy.DutyGrantLookup;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a member's duties grant in the bound studio, added to their role's set for each request
 * (plan 21 §3). Read with each request, not cached: duties are per person.
 */
@Component
class DutyGrants implements DutyGrantLookup {

  private final MemberDutyRepository duties;

  DutyGrants(MemberDutyRepository duties) {
    this.duties = duties;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Permission> grantedTo(UUID accountId) {
    Set<Permission> granted = EnumSet.noneOf(Permission.class);
    duties.findByAccount(accountId).forEach(held -> granted.addAll(held.duty().grants()));
    return granted;
  }
}
