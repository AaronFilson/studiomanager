package io.github.aaronfilson.studiomanager.schedule;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The roster. */
interface RosterShiftRepository extends JpaRepository<RosterShift, UUID> {

  /** Shifts that overlap [{@code from}, {@code to}), soonest first. */
  @Query(
      """
      select s from RosterShift s
      where s.startsAt < :to and s.endsAt > :from
      order by s.startsAt
      """)
  List<RosterShift> findOverlapping(Instant from, Instant to, Limit limit);
}
