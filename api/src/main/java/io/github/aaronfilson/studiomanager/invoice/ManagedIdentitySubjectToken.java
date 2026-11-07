package io.github.aaronfilson.studiomanager.invoice;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.google.auth.oauth2.ExternalAccountSupplierContext;
import com.google.auth.oauth2.IdentityPoolSubjectTokenSupplier;
import java.io.IOException;

/**
 * The subject token Google's workload identity federation trades for a Google token: an Entra token
 * for the API's managed identity, from {@code azure-identity}.
 *
 * <p>Container Apps serves managed-identity tokens from {@code IDENTITY_ENDPOINT}, not from the VM
 * metadata endpoint that gcloud's ready-made Azure credential file reads, so the token is supplied
 * here instead, through google-auth-library-java's {@link IdentityPoolSubjectTokenSupplier}
 * (checked against version 1.46.0: {@code String getSubjectToken(ExternalAccountSupplierContext)
 * throws IOException}, set with {@code IdentityPoolCredentials.Builder#setSubjectTokenSupplier}).
 */
final class ManagedIdentitySubjectToken implements IdentityPoolSubjectTokenSupplier {

  private final transient TokenCredential credential;
  private final String scope;

  ManagedIdentitySubjectToken(TokenCredential credential, String scope) {
    this.credential = credential;
    this.scope = scope;
  }

  @Override
  public String getSubjectToken(ExternalAccountSupplierContext context) throws IOException {
    try {
      AccessToken token = credential.getTokenSync(new TokenRequestContext().addScopes(scope));
      return token.getToken();
    } catch (RuntimeException e) {
      throw new IOException("Couldn't get the managed identity's token for Google", e);
    }
  }
}
