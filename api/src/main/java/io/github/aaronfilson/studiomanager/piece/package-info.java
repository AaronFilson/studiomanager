/**
 * Members' pieces from drop-off to pickup, their timelines and their photos.
 *
 * <p>On Hibernate (ADR 21). The SQL it keeps, each for a stated reason, is in {@code PieceQueries}:
 * the piece number counter and the hold-days upsert, the set-based move after a firing, the kiln
 * queue's {@code lateral} join, the first photo per piece, and the two cross-tenant sweeps.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.piece;

import org.jspecify.annotations.NullMarked;
