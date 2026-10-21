package io.github.aaronfilson.studiomanager.account;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.github.aaronfilson.studiomanager.platform.tenancy.AccountDirectory;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AccountNames implements AccountDirectory {

    private final JdbcClient jdbc;

    AccountNames(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> displayNames(Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("select id, display_name from account where id = any(:ids) and display_name is not null")
                .param("ids", accountIds.toArray(UUID[]::new))
                .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("display_name")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> emails(Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("select id, email from account where id = any(:ids) and email is not null")
                .param("ids", accountIds.toArray(UUID[]::new))
                .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("email")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
