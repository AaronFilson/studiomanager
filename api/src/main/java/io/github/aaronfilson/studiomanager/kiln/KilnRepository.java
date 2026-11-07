package io.github.aaronfilson.studiomanager.kiln;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's kilns; Hibernate adds the studio to every query ({@code @TenantId}). */
interface KilnRepository extends JpaRepository<Kiln, UUID> {

  @Query("select k from Kiln k order by k.site nulls first, k.name")
  List<Kiln> findAllInOrder();
}
