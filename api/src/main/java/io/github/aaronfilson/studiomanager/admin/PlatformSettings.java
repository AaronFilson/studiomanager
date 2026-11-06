package io.github.aaronfilson.studiomanager.admin;

import io.github.aaronfilson.studiomanager.platform.tenancy.PlatformAdmin;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

/**
 * The {@code platform_setting} table: a key, a JSON value and a version, with every change kept in
 * {@code platform_setting_change} by a trigger (V29). A null value means "no override".
 *
 * <p>Plain SQL rather than an entity: the value is free-form JSON, and the history is written by
 * the database, not by this code.
 */
@Repository
class PlatformSettings {

  /** One stored setting. {@code json} is null when it's back to its default. */
  record Setting(String key, @Nullable String json, long version) {}

  /** One change, from the history. */
  record Change(
      String key,
      long version,
      @Nullable String oldJson,
      @Nullable String newJson,
      UUID changedById,
      @Nullable String changedByName,
      OffsetDateTime changedAt) {}

  private final JdbcClient jdbc;
  private final TransactionOperations transactions;
  private final Clock clock;

  PlatformSettings(JdbcClient jdbc, TransactionOperations transactions, Clock clock) {
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.clock = clock;
  }

  /** Every stored setting, by key. Anyone may read them: every rate limit check does. */
  Map<String, Setting> readAll() {
    List<Setting> settings =
        transactions.execute(tx ->
            jdbc.sql("select key, value::text as json, version from platform_setting")
                .query(Setting.class)
                .list());
    return settings == null
        ? Map.of()
        : settings.stream().collect(Collectors.toMap(Setting::key, Function.identity()));
  }

  /**
   * Saves a setting's new value, or null to go back to its default, as the signed-in admin. The
   * database refuses it unless the call is bound as a platform admin.
   */
  void save(String key, @Nullable String json) {
    requireAdmin();
    UUID adminId = TenantContext.require().accountId();
    transactions.executeWithoutResult(tx ->
        jdbc.sql(
                """
                insert into platform_setting (key, value, version, changed_by, changed_at)
                values (:key, cast(:value as jsonb), 1, :by, :at)
                on conflict (key) do update set value = excluded.value,
                    version = platform_setting.version + 1,
                    changed_by = excluded.changed_by, changed_at = excluded.changed_at
                """)
            .param("key", key)
            .param("value", json)
            .param("by", adminId)
            .param("at", clock.instant().atOffset(ZoneOffset.UTC))
            .update());
  }

  /** The latest changes, newest first. Only admins can read them. */
  List<Change> history(int limit) {
    requireAdmin();
    List<Change> changes =
        transactions.execute(tx ->
            jdbc.sql(
                    """
                    select c.key, c.version, c.old_value::text as old_json,
                           c.new_value::text as new_json, c.changed_by as changed_by_id,
                           a.display_name as changed_by_name, c.changed_at
                    from platform_setting_change c
                    left join account a on a.id = c.changed_by
                    order by c.changed_at desc, c.version desc
                    limit :limit
                    """)
                .param("limit", limit)
                .query(Change.class)
                .list());
    return changes == null ? List.of() : changes;
  }

  private static void requireAdmin() {
    if (!PlatformAdmin.isBound()) {
      throw new AccessDeniedException("Only a platform admin can do this.");
    }
  }
}
