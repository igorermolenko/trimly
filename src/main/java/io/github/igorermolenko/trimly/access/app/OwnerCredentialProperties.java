package io.github.igorermolenko.trimly.access.app;

import io.github.igorermolenko.trimly.access.domain.OwnerCredentialPolicy;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The owner credential, bound from {@code trimly.owner-credential} (env {@code
 * TRIMLY_OWNER_CREDENTIAL}). trimly refuses to start when it is missing or breaks {@link
 * OwnerCredentialPolicy}.
 *
 * <p>The check runs after binding rather than in the constructor: a binding failure is reported
 * with the bound value, and the credential must never reach a log.
 */
@ConfigurationProperties("trimly")
public record OwnerCredentialProperties(String ownerCredential) implements InitializingBean {

  @Override
  public void afterPropertiesSet() {
    List<String> violations = OwnerCredentialPolicy.check(ownerCredential);
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "trimly.owner-credential (env TRIMLY_OWNER_CREDENTIAL) " + String.join(", ", violations));
    }
  }

  @Override
  public String toString() {
    return "OwnerCredentialProperties[ownerCredential=<redacted>]";
  }
}
