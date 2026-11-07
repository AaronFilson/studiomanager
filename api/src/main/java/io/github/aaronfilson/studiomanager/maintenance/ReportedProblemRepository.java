package io.github.aaronfilson.studiomanager.maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Problem reports. Staff see them all, anyone else only their own. */
interface ReportedProblemRepository extends JpaRepository<ReportedProblem, UUID> {

  /**
   * @param everyone true for staff, who see every report; otherwise only {@code reporter}'s
   * @param openOnly true to leave out resolved reports
   */
  @Query(
      """
      select p from ReportedProblem p
      where (:everyone = true or p.reportedBy = :reporter)
        and (:openOnly = false or p.resolvedAt is null)
      order by p.reportedAt desc
      """)
  List<ReportedProblem> findNewest(boolean everyone, UUID reporter, boolean openOnly, Limit limit);

  @Query(
      """
      select p from ReportedProblem p
      where p.id = :reportId and (:everyone = true or p.reportedBy = :reporter)
      """)
  Optional<ReportedProblem> findVisible(UUID reportId, boolean everyone, UUID reporter);
}
