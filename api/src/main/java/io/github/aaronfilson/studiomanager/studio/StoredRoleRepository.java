package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.StudioRole;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's roles; Hibernate adds the studio to every query ({@code @TenantId}). */
interface StoredRoleRepository extends JpaRepository<StoredRole, UUID> {

  @Query("select r from StoredRole r where r.base = :base and r.system = true")
  Optional<StoredRole> findSystemRole(StudioRole base);
}
