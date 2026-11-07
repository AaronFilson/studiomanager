package io.github.aaronfilson.studiomanager.studio;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** The bound studio's role permissions; Hibernate adds the studio to every query. */
interface StoredRolePermissionRepository
    extends JpaRepository<StoredRolePermission, StoredRolePermission.Key> {

  @Query("select p from StoredRolePermission p where p.key.roleId = :roleId")
  List<StoredRolePermission> findByRole(UUID roleId);
}
