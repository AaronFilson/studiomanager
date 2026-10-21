package io.github.aaronfilson.studiomanager.platform.storage;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param blobEndpoint the storage account's blob endpoint, such as
 *     {@code https://ststudiomgrdevabc123.blob.core.windows.net/}. When set, the API signs in with
 *     its managed identity and signs URLs with user-delegation keys. On Azure the account has no
 *     shared keys at all.
 * @param connectionString used when there is no endpoint: Azurite locally and in tests, signing
 *     URLs with the account key in the connection string
 * @param container the container that holds piece photos
 */
@ConfigurationProperties("studiomanager.storage")
public record StorageProperties(@Nullable String blobEndpoint, @Nullable String connectionString, String container) {

    boolean usesManagedIdentity() {
        return blobEndpoint != null && !blobEndpoint.isBlank();
    }

}
