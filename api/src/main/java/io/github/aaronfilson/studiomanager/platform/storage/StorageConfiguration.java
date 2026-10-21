package io.github.aaronfilson.studiomanager.platform.storage;

import java.util.List;

import com.azure.storage.blob.models.BlobCorsRule;
import com.azure.storage.blob.models.BlobServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    AzureBlobStore blobStore(StorageProperties properties) {
        return new AzureBlobStore(properties);
    }

    /**
     * Azurite starts empty, so locally the API creates the container and lets browsers upload to
     * it. On Azure, Terraform owns both (infra/modules/storage) and the API has no right to change them.
     * It runs in the background, so an Azurite that isn't running doesn't hold up startup while
     * the SDK retries.
     */
    @Bean
    ApplicationListener<ApplicationReadyEvent> prepareAzurite(StorageProperties properties, AzureBlobStore store) {
        return event -> {
            if (!properties.usesManagedIdentity()) {
                Thread.ofVirtual().name("prepare-azurite").start(() -> prepare(store));
            }
        };
    }

    private static void prepare(AzureBlobStore store) {
        try {
            store.container().createIfNotExists();
            store.service().setProperties(new BlobServiceProperties().setCors(List.of(new BlobCorsRule()
                    .setAllowedOrigins("*")
                    .setAllowedMethods("GET,HEAD,PUT,OPTIONS")
                    .setAllowedHeaders("*")
                    .setExposedHeaders("etag,x-ms-*")
                    .setMaxAgeInSeconds(3600))));
        } catch (RuntimeException e) {
            log.warn("Couldn't prepare Azurite, so photo uploads won't work until it's running "
                    + "(docker compose up -d): {}", e.getMessage());
        }
    }

}
