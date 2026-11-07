package io.github.aaronfilson.studiomanager.platform;

import java.util.List;
import java.util.function.Function;

/**
 * A list cut at a fixed number of rows, and whether anything was cut.
 *
 * <p>The query asks for {@code limit + 1} rows. Getting that many means there were more than the
 * limit, so the extra row is dropped and {@code truncated} is set. No count query is needed.
 *
 * @param items at most {@code limit} rows
 * @param truncated whether there were more rows than {@code limit}
 * @param limit the most rows this list returns
 */
public record Capped<T>(List<T> items, boolean truncated, int limit) {

  public Capped {
    items = List.copyOf(items);
  }

  /** The number of rows to ask the database for: one more than the limit. */
  public static int fetchSize(int limit) {
    return limit + 1;
  }

  /**
   * Builds the result from rows fetched with {@link #fetchSize(int)}.
   *
   * @param fetchedRows up to {@code limit + 1} rows, in display order
   */
  public static <T> Capped<T> fromFetched(List<T> fetchedRows, int limit) {
    boolean truncated = fetchedRows.size() > limit;
    List<T> keptRows = truncated ? fetchedRows.subList(0, limit) : fetchedRows;
    return new Capped<>(keptRows, truncated, limit);
  }

  /** The same cut, with each row transformed. */
  public <R> Capped<R> map(Function<? super T, ? extends R> transform) {
    List<R> transformedRows = items.stream().<R>map(transform).toList();
    return new Capped<>(transformedRows, truncated, limit);
  }

  /** The same cut, with rows replaced by {@code replacementRows} (for example, enriched copies). */
  public <R> Capped<R> withItems(List<R> replacementRows) {
    return new Capped<>(replacementRows, truncated, limit);
  }
}
