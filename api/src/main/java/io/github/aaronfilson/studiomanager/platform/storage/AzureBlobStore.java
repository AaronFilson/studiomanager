package io.github.aaronfilson.studiomanager.platform.storage;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.azure.core.util.Context;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.jspecify.annotations.Nullable;

/**
 * Azure Blob Storage, or Azurite locally.
 *
 * <p>On Azure the account has shared keys switched off, so URLs are user-delegation SAS: signed
 * with a key the API gets from Entra ID as its managed identity, which needs only the Storage
 * Blob Data Contributor role. With Azurite there is no Entra ID, so URLs are signed with the
 * well-known account key from the connection string instead. Either way, a URL is scoped to one
 * blob and expires.
 */
class AzureBlobStore implements BlobStore {

    /** A delegation key is valid for a day; one is fetched again when it can't cover a new URL. */
    private static final Duration DELEGATION_KEY_LIFETIME = Duration.ofDays(1);

    private final BlobServiceClient service;
    private final BlobContainerClient container;
    private final boolean userDelegation;
    private @Nullable UserDelegationKey delegationKey;

    AzureBlobStore(StorageProperties properties) {
        if (properties.usesManagedIdentity()) {
            this.service = new BlobServiceClientBuilder()
                    .endpoint(properties.blobEndpoint())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            this.userDelegation = true;
        } else {
            if (properties.connectionString() == null || properties.connectionString().isBlank()) {
                throw new IllegalStateException(
                        "Set studiomanager.storage.blob-endpoint (Azure) or studiomanager.storage.connection-string (Azurite)");
            }
            this.service = new BlobServiceClientBuilder().connectionString(properties.connectionString()).buildClient();
            this.userDelegation = false;
        }
        this.container = service.getBlobContainerClient(properties.container());
    }

    BlobServiceClient service() {
        return service;
    }

    BlobContainerClient container() {
        return container;
    }

    @Override
    public URI uploadUrl(String name, Instant expiresAt) {
        // Create only: once the API has checked a blob, the URL can't swap it for something else
        BlobSasPermission create = new BlobSasPermission().setCreatePermission(true);
        return sign(container.getBlobClient(name), new BlobServiceSasSignatureValues(utc(expiresAt), create));
    }

    @Override
    public URI readUrl(String name, String contentType) {
        Instant expiry = readExpiry(Instant.now());
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(utc(expiry),
                new BlobSasPermission().setReadPermission(true))
                .setContentType(contentType)
                .setCacheControl("private, max-age=3600");
        return sign(container.getBlobClient(name), values);
    }

    @Override
    public Optional<BlobInfo> info(String name) {
        try {
            BlobProperties properties = container.getBlobClient(name).getProperties();
            return Optional.of(new BlobInfo(properties.getBlobSize(),
                    properties.getContentType() == null ? "" : properties.getContentType()));
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public byte[] head(String name, int count) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(count);
        container.getBlobClient(name).downloadStreamWithResponse(out, new BlobRange(0, (long) count),
                new DownloadRetryOptions(), null, false, Duration.ofSeconds(30), Context.NONE);
        return out.toByteArray();
    }

    @Override
    public void delete(String name) {
        container.getBlobClient(name).deleteIfExists();
    }

    /**
     * Read URLs expire between one and two hours from now, on the hour. Every URL for a blob signed
     * within the same hour is identical, so it can be cached.
     */
    static Instant readExpiry(Instant now) {
        return now.truncatedTo(ChronoUnit.HOURS).plus(Duration.ofHours(2));
    }

    private URI sign(BlobClient blob, BlobServiceSasSignatureValues values) {
        String sas = userDelegation
                ? blob.generateUserDelegationSas(values, delegationKeyCovering(values.getExpiryTime()))
                : blob.generateSas(values);
        return URI.create(blob.getBlobUrl() + "?" + sas);
    }

    private synchronized UserDelegationKey delegationKeyCovering(OffsetDateTime expiry) {
        if (delegationKey == null || delegationKey.getSignedExpiry().isBefore(expiry)) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            delegationKey = service.getUserDelegationKey(now.minusMinutes(5), now.plus(DELEGATION_KEY_LIFETIME));
        }
        return delegationKey;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

}
