/**
 * Pending actions: changes an assistant proposes over MCP, which wait for a staff member to approve
 * or reject them in the app (docs/planning/17-mcp-and-api-tokens-plan.md, step 4).
 *
 * <p>Approving runs the owning module's normal service call as the person who approved, so every
 * usual check applies. Written in the Hibernate style of ADR 21, with no SQL of its own.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.proposal;

import org.jspecify.annotations.NullMarked;
