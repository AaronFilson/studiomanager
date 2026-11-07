package io.github.aaronfilson.studiomanager.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The bridge's RS256 signing keys, read from one PEM secret that holds one or more PKCS#8 private
 * keys. The first key signs; any after it are the keys being rotated out, published in the JWKS
 * only, so tokens they signed still verify until Entra's cached key set catches up (ADR 20).
 *
 * <p>Each key's ID is its RFC 7638 thumbprint, so it stays the same across restarts and replicas.
 */
public final class SigningKeys {

  private static final Pattern PRIVATE_KEY_BLOCK =
      Pattern.compile("-----BEGIN PRIVATE KEY-----([A-Za-z0-9+/=\\s]+)-----END PRIVATE KEY-----");
  private static final int MINIMUM_MODULUS_BITS = 2048;

  private final RSAKey signingKey;
  private final JWKSet keySet;

  private SigningKeys(RSAKey signingKey, JWKSet keySet) {
    this.signingKey = signingKey;
    this.keySet = keySet;
  }

  /** Parses the PEM secret. Fails with a message that never repeats the secret. */
  public static SigningKeys fromPem(String pem) {
    List<RSAKey> keys = new ArrayList<>();
    Matcher blocks = PRIVATE_KEY_BLOCK.matcher(pem);
    while (blocks.find()) {
      keys.add(toJwk(blocks.group(1)));
    }
    if (keys.isEmpty()) {
      throw new IllegalStateException(
          "studiomanager.bridge.signing-keys holds no PKCS#8 RSA private key"
              + " (-----BEGIN PRIVATE KEY-----).");
    }
    List<JWK> published = new ArrayList<>();
    published.add(keys.getFirst());
    for (RSAKey retiring : keys.subList(1, keys.size())) {
      published.add(retiring.toPublicJWK());
    }
    return new SigningKeys(keys.getFirst(), new JWKSet(published));
  }

  /** The key that signs new tokens, with its private half. */
  public RSAKey signingKey() {
    return signingKey;
  }

  /** The signing key and any retiring public keys. */
  public JWKSet keySet() {
    return keySet;
  }

  private static RSAKey toJwk(String base64Body) {
    try {
      byte[] der = Base64.getMimeDecoder().decode(base64Body.strip());
      KeyFactory factory = KeyFactory.getInstance("RSA");
      if (!(factory.generatePrivate(new PKCS8EncodedKeySpec(der))
          instanceof RSAPrivateCrtKey privateKey)) {
        throw new IllegalStateException("A signing key isn't an RSA key with its CRT parameters.");
      }
      if (privateKey.getModulus().bitLength() < MINIMUM_MODULUS_BITS) {
        throw new IllegalStateException("A signing key is shorter than 2048 bits.");
      }
      RSAPublicKey publicKey =
          (RSAPublicKey)
              factory.generatePublic(
                  new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
      return new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyUse(KeyUse.SIGNATURE)
          .algorithm(JWSAlgorithm.RS256)
          .keyIDFromThumbprint()
          .build();
    } catch (GeneralSecurityException | IllegalArgumentException | JOSEException exception) {
      throw new IllegalStateException("A signing key in the PEM secret can't be read.", exception);
    }
  }
}
