/**
 * Cross-cutting infrastructure every feature module builds on: tenancy (who is asking, in which
 * studio, enforced down to PostgreSQL row-level security), security, and HTTP errors.
 *
 * <p>An open module: feature modules may use any of its packages.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
@NullMarked
package io.github.aaronfilson.studiomanager.platform;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
