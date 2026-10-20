package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.sql.PreparedStatement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A JPA transaction manager that tells PostgreSQL who is asking.
 *
 * <p>At the start of every transaction it copies the bound {@link TenantContext} into the
 * settings {@code app.account_id} and {@code app.studio_id}, which the row-level security
 * policies read. {@code set_config(..., true)} is the function form of {@code SET LOCAL}: the
 * values vanish at commit or rollback, so a pooled connection can never carry one request's
 * tenant into the next.
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    private static final String BIND_TENANT =
            "select set_config('app.account_id', ?, true), set_config('app.studio_id', ?, true)";

    public TenantAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        EntityManagerHolder holder =
                (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
        EntityManager entityManager = holder.getEntityManager();
        try {
            bindTenant(entityManager);
        } catch (RuntimeException ex) {
            EntityTransaction begun = entityManager.getTransaction();
            if (begun.isActive()) {
                begun.rollback();
            }
            doCleanupAfterCompletion(transaction);
            throw new CannotCreateTransactionException("Could not bind the tenant to the transaction", ex);
        }
    }

    private static void bindTenant(EntityManager entityManager) {
        TenantContext context = TenantContext.current().orElse(null);
        String accountId = context == null ? "" : context.accountId().toString();
        String studioId = context == null || context.studioId() == null ? "" : context.studioId().toString();
        entityManager.unwrap(Session.class).doWork(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(BIND_TENANT)) {
                statement.setString(1, accountId);
                statement.setString(2, studioId);
                statement.execute();
            }
        });
    }

}
