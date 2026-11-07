package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Memberships. They carry no {@code @TenantId}, and a second row-level security policy lets anyone
 * read their own memberships in every studio, so each query here names its studio.
 */
interface StudioMembershipRepository extends JpaRepository<StudioMembership, UUID> {

  @Query(
      "select m.role from StudioMembership m where m.studioId = :studioId and m.accountId = :accountId")
  Optional<StudioRole> findRole(UUID studioId, UUID accountId);

  int countByStudioId(UUID studioId);

  List<StudioMembership> findByStudioId(UUID studioId);

  @Query(
      """
      select new io.github.aaronfilson.studiomanager.studio.MembershipView(s.id, s.name, m.role)
      from StudioMembership m join Studio s on s.id = m.studioId
      where m.accountId = :accountId
      order by s.name
      """)
  List<MembershipView> findMembershipsOf(UUID accountId);
}
