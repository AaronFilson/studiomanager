package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Arrays;
import java.util.Optional;

/** What a personal API token may do. Reading and each area's writes are separate. */
public enum TokenScope {
  READ("read"),
  WRITE_PIECES("write:pieces"),
  WRITE_KILNS("write:kilns"),
  WRITE_MAINTENANCE("write:maintenance"),
  WRITE_SCHEDULE("write:schedule"),
  WRITE_SUPPLIES("write:supplies"),
  WRITE_GLAZES("write:glazes"),
  WRITE_NOTICES("write:notices");

  private final String wireName;

  TokenScope(String wireName) {
    this.wireName = wireName;
  }

  public String wireName() {
    return wireName;
  }

  public static Optional<TokenScope> fromWireName(String name) {
    return Arrays.stream(values()).filter(s -> s.wireName.equals(name)).findFirst();
  }
}
