package io.github.aaronfilson.studiomanager.glaze;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Recipes, always fetched with their lines, since every view shows them. Hibernate adds the studio
 * to every query ({@code @TenantId}); row-level security also hides members' unshared recipes from
 * everyone but their owners.
 */
interface GlazeRecipeRepository extends JpaRepository<GlazeRecipe, UUID> {

  @Query("select r from GlazeRecipe r left join fetch r.lines where r.id = :recipeId")
  Optional<GlazeRecipe> findWithLines(UUID recipeId);

  @Query(
      """
      select r from GlazeRecipe r left join fetch r.lines
      where r.ownerKind = :ownerKind
      order by lower(r.name), r.id
      """)
  List<GlazeRecipe> findByOwnerKindWithLines(GlazeBook.OwnerKind ownerKind);

  @Query(
      """
      select r from GlazeRecipe r left join fetch r.lines
      where r.ownerAccountId = :ownerAccountId
      order by lower(r.name), r.id
      """)
  List<GlazeRecipe> findOwnedByWithLines(UUID ownerAccountId);

  /** Members' recipes shared with the studio by someone else. Only a member's recipe can be shared. */
  @Query(
      """
      select r from GlazeRecipe r left join fetch r.lines
      where r.sharedWithStudio = true and r.ownerAccountId <> :viewerAccountId
      order by lower(r.name), r.id
      """)
  List<GlazeRecipe> findSharedByOthersWithLines(UUID viewerAccountId);
}
