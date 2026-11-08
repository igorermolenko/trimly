package io.github.igorermolenko.trimly.redirect.api;

import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Puts {@link ServerRejectedAddressValve} on the host in place of every other error-report valve
 * (Tomcat's default and the one Spring Boot adds). The swap runs just before the application's
 * context starts, after the host and every customizer have installed theirs, so customizer order
 * does not matter.
 */
@Configuration(proxyBeanMethods = false)
class ServerRejectedAddressConfiguration {

  @Bean
  WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> serverRejectedAddressAnswers() {
    return factory ->
        factory.addContextCustomizers(context ->
            context.addLifecycleListener(event -> {
              if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
                useOnlyServerRejectedAddressValve(context.getParent());
              }
            }));
  }

  private static void useOnlyServerRejectedAddressValve(Container host) {
    Pipeline pipeline = host.getPipeline();
    boolean installed = false;
    for (Valve valve : pipeline.getValves()) {
      if (valve instanceof ServerRejectedAddressValve) {
        installed = true;
      } else if (valve instanceof ErrorReportValve) {
        pipeline.removeValve(valve);
      }
    }
    if (!installed) {
      pipeline.addValve(new ServerRejectedAddressValve());
    }
  }
}
