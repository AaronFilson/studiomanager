/**
 * Personal API tokens: made by staff, shown once, stored as a hash, and revoked at once.
 *
 * <p>On Hibernate (ADR 21). The only SQL it keeps is signing in with a token, which crosses tenants
 * through {@code app.use_api_token} ({@code ApiTokenQueries}).
 */
@NullMarked
package io.github.aaronfilson.studiomanager.apitoken;

import org.jspecify.annotations.NullMarked;
