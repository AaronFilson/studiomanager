package io.github.aaronfilson.studiomanager.auth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Holds the fixed clients (Entra federation and any optional pre-registered MCP client) plus a
 * bounded in-memory LRU cache of dynamically registered public PKCE MCP clients (ADR 26). Fixed
 * clients are never evicted or overwritten.
 */
final class McpClientRepository implements RegisteredClientRepository {

  private static final int MAX_DYNAMIC_CLIENTS = 1_000;

  private final String entraClientId;
  private final int maxDynamicClients;
  private final Map<String, RegisteredClient> fixedById;
  private final Map<String, RegisteredClient> fixedByClientId;
  private final Map<String, RegisteredClient> dynamicById = new LinkedHashMap<>(16, 0.75f, true);
  private final Map<String, String> dynamicIdByClientId = new HashMap<>();

  McpClientRepository(String entraClientId, List<RegisteredClient> fixedClients) {
    this(entraClientId, fixedClients, MAX_DYNAMIC_CLIENTS);
  }

  McpClientRepository(
      String entraClientId, List<RegisteredClient> fixedClients, int maxDynamicClients) {
    if (maxDynamicClients < 1) {
      throw new IllegalArgumentException("maxDynamicClients must be at least 1.");
    }
    this.entraClientId = entraClientId;
    this.maxDynamicClients = maxDynamicClients;
    Map<String, RegisteredClient> byId = new LinkedHashMap<>();
    Map<String, RegisteredClient> byClientId = new LinkedHashMap<>();
    for (RegisteredClient client : fixedClients) {
      byId.put(client.getId(), client);
      byClientId.put(client.getClientId(), client);
    }
    this.fixedById = Map.copyOf(byId);
    this.fixedByClientId = Map.copyOf(byClientId);
  }

  boolean isMcpClient(String clientId) {
    return !entraClientId.equals(clientId);
  }

  @Override
  public synchronized void save(RegisteredClient registeredClient) {
    if (fixedById.containsKey(registeredClient.getId())
        || fixedByClientId.containsKey(registeredClient.getClientId())) {
      throw new IllegalArgumentException("Fixed OAuth clients cannot be overwritten.");
    }
    RegisteredClient existing = dynamicById.get(registeredClient.getId());
    if (existing != null && !existing.getClientId().equals(registeredClient.getClientId())) {
      dynamicIdByClientId.remove(existing.getClientId());
    }
    if (dynamicById.size() >= maxDynamicClients && existing == null) {
      var oldest = dynamicById.entrySet().iterator().next();
      dynamicById.remove(oldest.getKey());
      dynamicIdByClientId.remove(oldest.getValue().getClientId());
    }
    dynamicById.put(registeredClient.getId(), registeredClient);
    dynamicIdByClientId.put(registeredClient.getClientId(), registeredClient.getId());
  }

  @Override
  public synchronized @Nullable RegisteredClient findById(String id) {
    RegisteredClient fixed = fixedById.get(id);
    return fixed != null ? fixed : dynamicById.get(id);
  }

  @Override
  public synchronized @Nullable RegisteredClient findByClientId(String clientId) {
    RegisteredClient fixed = fixedByClientId.get(clientId);
    if (fixed != null) {
      return fixed;
    }
    String id = dynamicIdByClientId.get(clientId);
    return id == null ? null : dynamicById.get(id);
  }
}
