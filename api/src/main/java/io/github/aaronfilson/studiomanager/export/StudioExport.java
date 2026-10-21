package io.github.aaronfilson.studiomanager.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.aaronfilson.studiomanager.platform.Csv;
import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The bound studio's own copy of everything it holds: one CSV per table, and the members' names
 * and addresses, in a ZIP. The pilot promises a studio this; it is also how a studio leaves.
 *
 * <p>The tables are found in the catalog (every table with a {@code studio_id}), so a table added
 * by a later migration is exported without anyone remembering to add it. Each is read with an
 * explicit filter on the studio, under row-level security as well, so one studio's export can't
 * hold another's rows. One repeatable-read transaction makes the files agree with each other.
 */
@Service
public class StudioExport {

    /** Binary columns are secrets or derived (an invite code's hash), not the studio's data. */
    private static final Set<String> LEFT_OUT_TYPES = Set.of("bytea");

    private final JdbcClient jdbc;
    private final AccountDirectory accounts;

    StudioExport(JdbcClient jdbc, AccountDirectory accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] zip(Instant now) {
        UUID studioId = TenantContext.require().studioId();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            List<String> tables = studioTables();
            write(zip, "README.txt", readme(now, tables));
            write(zip, "studio.csv", table("select * from studio where id = :studioId", studioId));
            for (String table : tables) {
                write(zip, table + ".csv",
                        table("select * from \"%s\" where studio_id = :studioId order by 1".formatted(table), studioId));
            }
            write(zip, "members.csv", members());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Every table in the public schema with a studio_id column. */
    List<String> studioTables() {
        return jdbc.sql("""
                select c.relname
                from pg_class c
                join pg_namespace n on n.oid = c.relnamespace
                join pg_attribute a on a.attrelid = c.oid and a.attname = 'studio_id' and not a.attisdropped
                where n.nspname = 'public' and c.relkind = 'r'
                order by c.relname
                """).query(String.class).list();
    }

    private String table(String sql, UUID studioId) {
        Csv csv = new Csv();
        List<Integer> columns = new ArrayList<>();
        jdbc.sql(sql).param("studioId", studioId).query((ResultSet rs) -> {
            if (columns.isEmpty()) {
                csv.row(header(rs.getMetaData(), columns));
            }
            List<Object> row = new ArrayList<>(columns.size());
            for (int column : columns) {
                row.add(cell(rs, column));
            }
            csv.row(row);
        });
        // An empty table still gets its header, from the statement's metadata
        return columns.isEmpty() ? headerOnly(sql, studioId) : csv.toString();
    }

    private String headerOnly(String sql, UUID studioId) {
        return jdbc.sql("select * from (" + sql + ") t limit 0").param("studioId", studioId)
                .query((ResultSetExtractor<String>) rs -> new Csv().row(header(rs.getMetaData(), new ArrayList<>()))
                        .toString());
    }

    private static List<String> header(ResultSetMetaData meta, List<Integer> columns) throws SQLException {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (!LEFT_OUT_TYPES.contains(meta.getColumnTypeName(i))) {
                columns.add(i);
                names.add(meta.getColumnName(i));
            }
        }
        return names;
    }

    private static Object cell(ResultSet rs, int column) throws SQLException {
        String type = rs.getMetaData().getColumnTypeName(column);
        return switch (type) {
            case "int2", "int4", "int8", "numeric", "float4", "float8" -> rs.getObject(column);
            case "timestamptz" -> {
                OffsetDateTime at = rs.getObject(column, OffsetDateTime.class);
                yield at == null ? null : at.toInstant().toString();
            }
            default -> {
                if (type.startsWith("_")) {
                    // An array, such as a checklist: one cell, its lines separated
                    Array array = rs.getArray(column);
                    yield array == null ? null
                            : Arrays.stream((Object[]) array.getArray()).map(String::valueOf)
                                    .collect(Collectors.joining("; "));
                }
                yield rs.getString(column);
            }
        };
    }

    private record Membership(UUID accountId, String role, OffsetDateTime createdAt) {
    }

    private String members() {
        List<Membership> rows = jdbc.sql("select account_id, role, created_at from studio_membership order by created_at")
                .query(Membership.class).list();
        List<UUID> ids = rows.stream().map(Membership::accountId).toList();
        Map<UUID, String> names = accounts.displayNames(ids);
        Map<UUID, String> emails = accounts.emails(ids);
        Csv csv = new Csv().row(List.of("account_id", "name", "email", "role", "joined_at"));
        for (Membership row : rows) {
            csv.row(Arrays.asList(row.accountId(), names.get(row.accountId()), emails.get(row.accountId()), row.role(),
                    row.createdAt().toInstant().toString()));
        }
        return csv.toString();
    }

    private static String readme(Instant now, List<String> tables) {
        return """
                Studio Manager export, taken %s (UTC).

                studio.csv is the studio itself, and members.csv is everyone in it with their name and email
                address. Every other file is one table of the studio's data, named after the table, with a
                header row. Times are UTC, in ISO 8601. Lists, such as a checklist, are one cell with the
                lines separated by "; ". Amounts are in the studio's currency.

                Photos are listed in piece_photo.csv; the images themselves aren't in this file.

                Tables: studio, %s, members
                """.formatted(now, String.join(", ", tables));
    }

    private static void write(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

}
