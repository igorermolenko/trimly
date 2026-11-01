package io.github.igorermolenko.trimly.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The single application clock; inject {@link Clock} instead of calling {@code now()} directly. */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
