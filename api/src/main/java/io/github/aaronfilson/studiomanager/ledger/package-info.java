/**
 * The studio's books: a double-entry journal, members' balances, stored value, processor payouts,
 * tax rates, and payables (what the studio owes the artists and instructors it pays).
 *
 * <p>On Hibernate (ADR 21). Postings and every record that carries one are entities with
 * {@code @TenantId}. Balances, statements, the 1099 list and the export stay SQL, in {@code
 * LedgerQueries} and {@code PayablesQueries}, along with the two upserts (tax rates) and the
 * stored-value balance's {@code lateral} join.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.ledger;

import org.jspecify.annotations.NullMarked;
