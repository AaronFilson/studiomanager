package io.github.aaronfilson.studiomanager.glaze;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GlazecalcConnectionRepository extends JpaRepository<GlazecalcConnection, UUID> {

  Optional<GlazecalcConnection> findByAccountId(UUID accountId);
}
