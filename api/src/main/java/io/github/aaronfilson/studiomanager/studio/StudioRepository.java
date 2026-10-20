package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface StudioRepository extends JpaRepository<Studio, UUID> {
}
