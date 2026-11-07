/**
 * The demo studio, Demo Clay Co. (docs/planning/20, section 7, item 3): resetting it to a seeded
 * state, and a short-lived read token for connecting an assistant during a demo.
 *
 * <p>The seed goes through the other modules' services, as people would, never SQL, so it breaks
 * loudly when a service's rules change. Its people are accounts with the issuer {@link
 * DemoPeople#ISSUER}, which no identity provider issues, so nobody can sign in as them. The demo
 * studio is the studio the seeded owner owns; a reset deletes only that.
 *
 * <p>Only platform admins reach it, and nothing depends on it.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.demo;

import org.jspecify.annotations.NullMarked;
