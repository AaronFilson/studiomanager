package io.github.aaronfilson.studiomanager.piece;

import io.github.aaronfilson.studiomanager.apitoken.ApiTokens;
import io.github.aaronfilson.studiomanager.platform.storage.BlobStore;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Turns pieces into what the API shows: people's names, and photos with signed URLs. A whole list
 * is a fixed number of queries: one for the first photos, one for the names.
 */
@Component
class PieceViews {

  private final PieceQueries queries;
  private final PiecePhotoRepository photos;
  private final AccountDirectory accounts;
  private final BlobStore blobs;
  private final ApiTokens tokens;

  PieceViews(
      PieceQueries queries,
      PiecePhotoRepository photos,
      AccountDirectory accounts,
      BlobStore blobs,
      ApiTokens tokens) {
    this.queries = queries;
    this.photos = photos;
    this.accounts = accounts;
    this.blobs = blobs;
    this.tokens = tokens;
  }

  /** Pieces as lists show them: each with its first photo and no timeline. */
  List<PieceView> listed(List<Piece> pieces) {
    if (pieces.isEmpty()) {
      return List.of();
    }
    UUID studioId = TenantContext.require().requireStudioId();
    Map<UUID, PieceView.Photo> firstPhotos = new HashMap<>();
    for (PieceQueries.FirstPhoto first :
        queries.firstReadyPhotos(pieces.stream().map(Piece::id).toList())) {
      firstPhotos.put(
          first.pieceId(),
          photo(studioId, first.pieceId(), first.photoId(), first.uploadedBy(), first.readyAt()));
    }
    Map<UUID, String> names =
        accounts.displayNames(pieces.stream().map(Piece::makerAccountId).toList());
    List<PieceView> views = new ArrayList<>(pieces.size());
    for (Piece piece : pieces) {
      PieceView.Photo first = firstPhotos.get(piece.id());
      views.add(
          piece.toView(
              names.get(piece.makerAccountId()),
              first == null ? List.of() : List.of(first),
              List.of()));
    }
    return views;
  }

  /** One piece with every photo and its whole timeline. */
  PieceView detailed(Piece piece, List<PieceEvent> timeline) {
    UUID studioId = TenantContext.require().requireStudioId();
    List<PieceView.Photo> allPhotos =
        photos.findReady(piece.id()).stream()
            .map(found ->
                photo(studioId, piece.id(), found.id(), found.uploadedBy(), found.readyAt()))
            .toList();
    Set<UUID> people = new HashSet<>();
    people.add(piece.makerAccountId());
    for (PieceEvent event : timeline) {
      UUID actor = event.actorAccountId();
      if (actor != null) {
        people.add(actor);
      }
    }
    Map<UUID, String> names = accounts.displayNames(people);
    Map<UUID, String> tokenNames =
        tokens.names(
            timeline.stream().map(PieceEvent::viaTokenId).filter(Objects::nonNull).toList());
    List<PieceView.Event> namedTimeline =
        timeline.stream()
            .map(event -> {
              UUID actor = event.actorAccountId();
              UUID viaToken = event.viaTokenId();
              return event.toView(
                  actor == null ? null : names.get(actor),
                  viaToken == null ? null : tokenNames.get(viaToken));
            })
            .toList();
    return piece.toView(names.get(piece.makerAccountId()), allPhotos, namedTimeline);
  }

  private PieceView.Photo photo(
      UUID studioId, UUID pieceId, UUID photoId, UUID uploadedBy, Instant readyAt) {
    return new PieceView.Photo(
        photoId,
        blobs.readUrl(PhotoBlobs.full(studioId, pieceId, photoId), PhotoBlobs.CONTENT_TYPE),
        blobs.readUrl(PhotoBlobs.thumbnail(studioId, pieceId, photoId), PhotoBlobs.CONTENT_TYPE),
        uploadedBy,
        readyAt);
  }
}
