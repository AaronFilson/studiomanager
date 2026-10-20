package io.github.aaronfilson.studiomanager.platform.tenancy;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TenancyConfiguration {

    /** Replaces Spring Boot's default JPA transaction manager. */
    @Bean
    TenantAwareJpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new TenantAwareJpaTransactionManager(entityManagerFactory);
    }

}
