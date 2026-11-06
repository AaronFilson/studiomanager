package io.github.aaronfilson.studiomanager.platform.tenancy;

import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Binds the tenant to every transaction, for row-level security and for Hibernate's
 * {@code @TenantId}, and fills the audit columns (ADR 21).
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditTimes", auditorAwareRef = "auditAccounts")
class TenancyConfiguration {

  /** Replaces Spring Boot's default JPA transaction manager. */
  @Bean
  TenantAwareJpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    return new TenantAwareJpaTransactionManager(entityManagerFactory);
  }

  @Bean
  HibernatePropertiesCustomizer studioTenantResolver() {
    return properties ->
        properties.put(
            MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new StudioTenantResolver());
  }

  /** {@code @CreatedDate} and {@code @LastModifiedDate} read the application's one clock. */
  @Bean
  DateTimeProvider auditTimes(Clock clock) {
    return () -> Optional.of(clock.instant());
  }

  /** {@code @CreatedBy} is the bound account: a person, or a job's system account. */
  @Bean
  AuditorAware<UUID> auditAccounts() {
    return () -> TenantContext.current().map(TenantContext::accountId);
  }
}
