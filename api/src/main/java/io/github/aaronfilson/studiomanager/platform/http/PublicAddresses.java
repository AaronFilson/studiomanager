package io.github.aaronfilson.studiomanager.platform.http;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Whether an address is on the public internet. The SSRF filter sends only to addresses that are,
 * so a URL a studio types can't reach the cloud metadata endpoint, the database or anything else on
 * a private network.
 *
 * <p>It works on the resolved bytes, never the hostname's spelling, so {@code 2130706433}, {@code
 * 0x7f.1} and {@code [::ffff:127.0.0.1]} are all caught as loopback. IPv6 forms that carry an IPv4
 * address (mapped, compatible, NAT64, 6to4) are judged by the address they carry.
 */
public final class PublicAddresses {

  /**
   * A class rather than a record: a record's equals would compare the prefix array by reference.
   */
  private static final class Block {

    private final byte[] prefix;
    private final int bits;
    private final String why;

    private Block(byte[] prefix, int bits, String why) {
      this.prefix = prefix;
      this.bits = bits;
      this.why = why;
    }

    String why() {
      return why;
    }

    static Block of(String cidr, String why) {
      String[] parts = cidr.split("/", -1);
      try {
        return new Block(
            InetAddress.getByName(parts[0]).getAddress(), Integer.parseInt(parts[1]), why);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException(cidr, e);
      }
    }

    boolean contains(byte[] address) {
      if (address.length != prefix.length) {
        return false;
      }
      int whole = bits / 8;
      if (!Arrays.equals(address, 0, whole, prefix, 0, whole)) {
        return false;
      }
      int rest = bits % 8;
      if (rest == 0) {
        return true;
      }
      int mask = 0xFF << (8 - rest) & 0xFF;
      return (address[whole] & mask) == (prefix[whole] & mask);
    }
  }

  private static final List<Block> V4 =
      List.of(
          Block.of("0.0.0.0/8", "is a 'this network' address"),
          Block.of("10.0.0.0/8", "is a private address"),
          Block.of("100.64.0.0/10", "is a carrier-grade NAT address"),
          Block.of("127.0.0.0/8", "is a loopback address"),
          Block.of(
              "169.254.0.0/16", "is a link-local address, such as the cloud metadata endpoint"),
          Block.of("172.16.0.0/12", "is a private address"),
          Block.of("192.0.0.0/24", "is reserved"),
          Block.of("192.0.2.0/24", "is reserved for documentation"),
          Block.of("192.88.99.0/24", "is reserved"),
          Block.of("192.168.0.0/16", "is a private address"),
          Block.of("198.18.0.0/15", "is reserved for benchmarking"),
          Block.of("198.51.100.0/24", "is reserved for documentation"),
          Block.of("203.0.113.0/24", "is reserved for documentation"),
          Block.of("224.0.0.0/4", "is a multicast address"),
          Block.of("240.0.0.0/4", "is reserved"));

  private static final List<Block> V6 =
      List.of(
          Block.of("::/128", "is the unspecified address"),
          Block.of("::1/128", "is a loopback address"),
          Block.of("100::/64", "is a discard address"),
          Block.of("2001::/32", "is a Teredo tunnel address"),
          Block.of("2001:db8::/32", "is reserved for documentation"),
          Block.of("fc00::/7", "is a private (unique local) address"),
          Block.of("fe80::/10", "is a link-local address"),
          Block.of("fec0::/10", "is a site-local address"),
          Block.of("ff00::/8", "is a multicast address"));

  private static final Block MAPPED = Block.of("::ffff:0:0/96", "");
  private static final Block COMPATIBLE = Block.of("::/96", "");
  private static final Block NAT64 = Block.of("64:ff9b::/96", "");
  private static final Block SIX_TO_FOUR = Block.of("2002::/16", "");

  private PublicAddresses() {}

  /** Why the address isn't public, such as "is a private address", or empty if it is. */
  public static Optional<String> whyNot(InetAddress address) {
    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address) {
      return check(V4, bytes);
    }
    if (address instanceof Inet6Address) {
      // Forms that carry an IPv4 address are judged by it
      if (MAPPED.contains(bytes)
          || NAT64.contains(bytes)
          || (COMPATIBLE.contains(bytes)
              && !V6.get(0).contains(bytes)
              && !V6.get(1).contains(bytes))) {
        return check(V4, Arrays.copyOfRange(bytes, 12, 16));
      }
      if (SIX_TO_FOUR.contains(bytes)) {
        return check(V4, Arrays.copyOfRange(bytes, 2, 6));
      }
      return check(V6, bytes);
    }
    return Optional.of("isn't an IP address");
  }

  private static Optional<String> check(List<Block> blocks, byte[] bytes) {
    return blocks.stream().filter(block -> block.contains(bytes)).map(Block::why).findFirst();
  }
}
