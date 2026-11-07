package io.github.igorermolenko.trimly.link.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

/** AC-01, ADR-0002: production short codes cannot be predicted from earlier ones. */
class LinkConfigurationTest {

  @Test
  void productionShortCodesComeFromASecureRandomSource() {
    assertThat(LinkConfiguration.shortCodeRandom()).isInstanceOf(SecureRandom.class);
  }
}
