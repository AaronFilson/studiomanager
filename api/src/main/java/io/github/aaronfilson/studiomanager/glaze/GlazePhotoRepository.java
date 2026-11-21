package io.github.aaronfilson.studiomanager.glaze;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface GlazePhotoRepository extends JpaRepository<GlazePhoto, UUID> {

  Optional<GlazePhoto> findByIdAndRecipeId(UUID id, UUID recipeId);

  @Query("select p.id from GlazePhoto p where p.recipeId = :recipeId")
  List<UUID> findIdsByRecipeId(UUID recipeId);

  @Query(
      """
      select p from GlazePhoto p
      where p.recipeId = :recipeId
        and p.status = io.github.aaronfilson.studiomanager.glaze.GlazePhoto.Status.READY
      order by p.createdAt, p.id
      """)
  List<GlazePhoto> findReady(UUID recipeId);

  @Query(
      """
      select p from GlazePhoto p
      where p.recipeId in :recipeIds
        and p.status = io.github.aaronfilson.studiomanager.glaze.GlazePhoto.Status.READY
      order by p.createdAt, p.id
      """)
  List<GlazePhoto> findReadyForRecipes(Collection<UUID> recipeIds);

  @Query(
      """
      select count(p) from GlazePhoto p
      where p.recipeId = :recipeId
        and (p.status = io.github.aaronfilson.studiomanager.glaze.GlazePhoto.Status.READY
             or p.createdAt > :windowStart)
      """)
  long countTaken(UUID recipeId, Instant windowStart);

  @Query(
      """
      select count(p) from GlazePhoto p
      where p.recipeId = :recipeId
        and p.status = io.github.aaronfilson.studiomanager.glaze.GlazePhoto.Status.READY
      """)
  long countReady(UUID recipeId);
}
