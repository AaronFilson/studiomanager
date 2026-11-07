package io.github.aaronfilson.studiomanager.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Credited hours. */
interface HoursCreditRepository extends JpaRepository<HoursCredit, UUID> {

  @Query(
      "select h from HoursCredit h where h.accountId = :accountId order by h.creditedAt desc")
  List<HoursCredit> findNewestFor(UUID accountId, Limit limit);

  /** Everything credited to someone, past any cap on the entries listed: one aggregate query. */
  @Query("select coalesce(sum(h.minutes), 0L) from HoursCredit h where h.accountId = :accountId")
  long totalMinutes(UUID accountId);
}
