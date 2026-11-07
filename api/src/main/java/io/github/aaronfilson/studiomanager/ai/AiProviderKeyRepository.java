package io.github.aaronfilson.studiomanager.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** The studios' AI keys, each found by its studio's id. */
interface AiProviderKeyRepository extends JpaRepository<AiProviderKey, UUID> {}
