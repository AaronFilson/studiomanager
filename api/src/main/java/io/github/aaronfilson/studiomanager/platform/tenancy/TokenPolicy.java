package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * What a personal API token may reach, decided in one place by the route's area (the segment after
 * {@code /api/v1/studios/{studioId}/}) and the HTTP method. It fails closed: an area that isn't
 * listed here is refused, and a test checks every area in the API contract is listed.
 *
 * <p>Money, imports, the export, webhooks and the tokens themselves are never reachable with a
 * token, whatever its scopes (docs/planning/17-mcp-and-api-tokens-plan.md).
 */
public final class TokenPolicy {

  /** How an area is reached with a token: read only, readable with a write scope, or never. */
  public sealed interface Access {}

  public record ReadOnly() implements Access {}

  public record Writable(TokenScope scope) implements Access {}

  public record Never(String why) implements Access {}

  private static final Access READ_ONLY = new ReadOnly();
  private static final Access MONEY = new Never("Tokens can't reach the studio's money.");
  private static final Access ADMIN =
      new Never("Tokens can't change who has access, or how the studio is set up.");

  static final Map<String, Access> AREAS =
      Map.ofEntries(
          Map.entry("", READ_ONLY),
          // The MCP endpoint is POST-only JSON-RPC, and every tool on it only reads (McpTools)
          Map.entry("mcp", new Writable(TokenScope.READ)),
          Map.entry("today", READ_ONLY),
          Map.entry("activity", READ_ONLY),
          Map.entry("members", READ_ONLY),
          Map.entry("fee-schedule", READ_ONLY),
          Map.entry("piece-policy", READ_ONLY),
          Map.entry("fee-quotes", new Writable(TokenScope.WRITE_PIECES)),
          Map.entry("pieces", new Writable(TokenScope.WRITE_PIECES)),
          Map.entry("kilns", new Writable(TokenScope.WRITE_KILNS)),
          Map.entry("firings", new Writable(TokenScope.WRITE_KILNS)),
          Map.entry("maintenance", new Writable(TokenScope.WRITE_MAINTENANCE)),
          Map.entry("duties", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("certification-types", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("certifications", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("resources", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("bookings", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("shifts", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("hours", new Writable(TokenScope.WRITE_SCHEDULE)),
          Map.entry("supplies", new Writable(TokenScope.WRITE_SUPPLIES)),
          Map.entry("purchase-requests", new Writable(TokenScope.WRITE_SUPPLIES)),
          Map.entry("glazes", new Writable(TokenScope.WRITE_GLAZES)),
          Map.entry("notifications", new Writable(TokenScope.WRITE_NOTICES)),
          Map.entry("notices", new Writable(TokenScope.WRITE_NOTICES)),
          Map.entry("balances", MONEY),
          Map.entry("payments", MONEY),
          Map.entry("stored-values", MONEY),
          Map.entry("gift-cards", MONEY),
          Map.entry("stored-value-movements", MONEY),
          Map.entry("payouts", MONEY),
          Map.entry("clearing", MONEY),
          Map.entry("ledger", MONEY),
          Map.entry("tax-rates", MONEY),
          Map.entry("payees", MONEY),
          Map.entry("payment-runs", MONEY),
          Map.entry("consignment-items", MONEY),
          Map.entry("consignment-sales", MONEY),
          Map.entry("payables", MONEY),
          Map.entry("my-consignment", MONEY),
          Map.entry("invites", ADMIN),
          Map.entry("imports", ADMIN),
          Map.entry("webhooks", ADMIN),
          Map.entry("api-tokens", ADMIN),
          Map.entry("ai-key", ADMIN),
          // Reading an invoice spends the studio's AI key, and confirming one moves stock and money
          Map.entry(
              "invoices",
              new Never("Invoices are read and confirmed while signed in, not with a token.")),
          Map.entry(
              "export",
              new Never("Download the studio's data while signed in, not with a token.")));

  private TokenPolicy() {}

  /** Why a token with these scopes can't make this request, or empty if it can. */
  public static Optional<String> refusal(
      Set<TokenScope> scopes, String method, @Nullable String area) {
    Access access =
        area == null
            ? new Never("Tokens work only inside their studio.")
            : AREAS.getOrDefault(area, new Never("Tokens can't reach this part of the API."));
    boolean reading = method.equals("GET") || method.equals("HEAD");
    // Exhaustive over the sealed Access: a new kind of access won't compile until it's handled
    return switch (access) {
      case Never(String why) -> Optional.of(why);
      case ReadOnly() -> reading ? needsRead(scopes) : Optional.of("Tokens can only read this.");
      case Writable(TokenScope writeScope) ->
          reading ? needsRead(scopes) : needsScope(scopes, writeScope);
    };
  }

  /** Reading anything a token can reach needs the read scope. */
  private static Optional<String> needsRead(Set<TokenScope> scopes) {
    return scopes.contains(TokenScope.READ)
        ? Optional.empty()
        : Optional.of("This token needs the read scope.");
  }

  private static Optional<String> needsScope(Set<TokenScope> scopes, TokenScope writeScope) {
    return scopes.contains(writeScope)
        ? Optional.empty()
        : Optional.of("This token needs the %s scope.".formatted(writeScope.wireName()));
  }
}
