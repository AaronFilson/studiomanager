package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** The bound studio's access log; Hibernate adds the studio to every query ({@code @TenantId}). */
interface AccessChangeRepository extends JpaRepository<AccessChange, UUID> {}
