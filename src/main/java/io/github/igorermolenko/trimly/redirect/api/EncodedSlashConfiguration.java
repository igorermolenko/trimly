package io.github.igorermolenko.trimly.redirect.api;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat answers an address with an encoded slash ({@code %2F}) with its own 400 before the
 * application sees it. Passing it through, still encoded, lets Spring Security's firewall reject it
 * like any other unusable address, so a visitor gets "link not found" (AC-16b). The slash is never
 * decoded into a path separator.
 */
@Configuration(proxyBeanMethods = false)
class EncodedSlashConfiguration {

  @Bean
  WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> encodedSlashReachesTheFirewall() {
    return factory ->
        factory.addConnectorCustomizers(
            connector -> connector.setEncodedSolidusHandling("passthrough"));
  }
}
