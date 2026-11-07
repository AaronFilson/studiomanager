/**
 * The studio's own AI key: set, checked and removed by the owner, and kept encrypted.
 *
 * <p>On Hibernate (ADR 21). The only SQL it keeps is saving the key, an upsert ({@code
 * AiKeyQueries}).
 */
@NullMarked
package io.github.aaronfilson.studiomanager.ai;

import org.jspecify.annotations.NullMarked;
