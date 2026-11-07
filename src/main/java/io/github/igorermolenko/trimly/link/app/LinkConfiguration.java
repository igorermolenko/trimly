package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class LinkConfiguration {

  @Bean
  ShortCodeGenerator shortCodeGenerator() {
    return ShortCodeGenerator.from(shortCodeRandom());
  }

  /** Codes cannot be predicted from earlier ones (AC-01, ADR-0002). */
  static RandomGenerator shortCodeRandom() {
    return new SecureRandom();
  }
}
