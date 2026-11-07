package io.github.aaronfilson.studiomanager.piece;

/**
 * A photo is PENDING from handing out its upload URLs until both images are checked, then READY.
 */
enum PhotoStatus {
  PENDING,
  READY
}
