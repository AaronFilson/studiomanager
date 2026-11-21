package io.github.aaronfilson.studiomanager.glaze;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface GlazeMixRepository extends JpaRepository<GlazeMix, UUID> {

  @Query(
      """
      select m.id from GlazeMix m
      where m.recipeId = :recipeId
      order by m.mixedAt desc, m.id desc
      """)
  List<UUID> findNewestIdsByRecipeId(UUID recipeId, Limit limit);

  @Query(
      """
      select m from GlazeMix m left join fetch m.readings
      where m.id in :ids
      order by m.mixedAt desc, m.id desc
      """)
  List<GlazeMix> findByIdsWithReadings(Collection<UUID> ids);

  @Query(
      """
      select m from GlazeMix m left join fetch m.readings
      where m.id = :mixId and m.recipeId = :recipeId
      """)
  Optional<GlazeMix> findByIdAndRecipeIdWithReadings(UUID mixId, UUID recipeId);
}
