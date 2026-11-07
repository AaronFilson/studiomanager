package io.github.aaronfilson.studiomanager.studio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's access log; Hibernate adds the studio to every query ({@code @TenantId}). */
interface AccessChangeRepository extends JpaRepository<AccessChange, UUID> {

  /** The newest changes first; ids are time-ordered, so they break ties within one instant. */
  @Query("select c from AccessChange c order by c.at desc, c.id desc")
  List<AccessChange> findNewestFirst(Limit limit);
}
