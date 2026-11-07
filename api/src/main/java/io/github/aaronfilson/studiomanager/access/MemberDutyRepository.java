package io.github.aaronfilson.studiomanager.access;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's duties; Hibernate adds the studio to every query ({@code @TenantId}). */
interface MemberDutyRepository extends JpaRepository<MemberDuty, MemberDuty.Key> {

  @Query("select d from MemberDuty d where d.key.accountId = :accountId")
  List<MemberDuty> findByAccount(UUID accountId);
}
