package io.github.aaronfilson.studiomanager.access;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's certification types; Hibernate adds the studio ({@code @TenantId}). */
interface CertificationTypeRepository extends JpaRepository<CertificationTypeEntity, UUID> {

  @Query("select t from CertificationTypeEntity t order by t.name")
  List<CertificationTypeEntity> findAllByName();
}
