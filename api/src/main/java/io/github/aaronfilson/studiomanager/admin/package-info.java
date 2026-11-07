/**
 * Running the service: platform settings changed live from the admin page, starting with the rate
 * limits and their boost (docs/planning/20, section 7; ADR 24).
 *
 * <p>Only people with the {@code Platform.Admin} app role reach it, and the database refuses writes
 * to the settings from anyone else.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.admin;

import org.jspecify.annotations.NullMarked;
