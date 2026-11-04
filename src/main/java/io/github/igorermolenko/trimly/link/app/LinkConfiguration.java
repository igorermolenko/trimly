package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class LinkConfiguration {

  /** Codes cannot be predicted from earlier ones (AC-01). */
  @Bean
  ShortCodeGenerator shortCodeGenerator() {
    return ShortCodeGenerator.from(new SecureRandom());
  }
}
