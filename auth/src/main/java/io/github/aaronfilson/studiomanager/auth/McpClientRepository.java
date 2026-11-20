package io.github.aaronfilson.studiomanager.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Holds the fixed clients (Entra federation and any optional pre-registered MCP client) plus a
 * bounded in-memory ring of dynamically registered public PKCE MCP clients (ADR 26). Fixed clients
 * are never evicted or overwritten.
 */
final class McpClientRepository implements RegisteredClientRepository {

  private static final int MAX_DYNAMIC_CLIENTS = 1_000;

  private final String entraClientId;
  private final Map<String, RegisteredClient> fixedById;
  private final Map<String, RegisteredClient> fixedByClientId;
  private final Map<String, RegisteredClient> dynamicById = new LinkedHashMap<>();
  private final Map<String, RegisteredClient> dynamicByClientId = new LinkedHashMap<>();

  McpClientRepository(String entraClientId, List<RegisteredClient> fixedClients) {
    this.entraClientId = entraClientId;
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
    if (dynamicById.size() >= MAX_DYNAMIC_CLIENTS
        && !dynamicById.containsKey(registeredClient.getId())) {
      var oldest = dynamicById.entrySet().iterator().next();
      dynamicById.remove(oldest.getKey());
      dynamicByClientId.remove(oldest.getValue().getClientId());
    }
    dynamicById.put(registeredClient.getId(), registeredClient);
    dynamicByClientId.put(registeredClient.getClientId(), registeredClient);
  }

  @Override
  public synchronized @Nullable RegisteredClient findById(String id) {
    RegisteredClient fixed = fixedById.get(id);
    return fixed != null ? fixed : dynamicById.get(id);
  }

  @Override
  public synchronized @Nullable RegisteredClient findByClientId(String clientId) {
    RegisteredClient fixed = fixedByClientId.get(clientId);
    return fixed != null ? fixed : dynamicByClientId.get(clientId);
  }
}
