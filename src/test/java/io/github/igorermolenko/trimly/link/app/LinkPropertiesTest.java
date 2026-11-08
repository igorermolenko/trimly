package io.github.igorermolenko.trimly.link.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

class LinkPropertiesTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @Test
  void bindsThePublicBaseUrlAndOwnHosts() {
    runner
        .withPropertyValues(
            "trimly.public-base-url=https://trimly.example.test",
            "trimly.own-hosts=trimly.example.test,www.trimly.example.test")
        .run(context -> {
          LinkProperties properties = context.getBean(LinkProperties.class);
          assertThat(properties.publicBaseUrl())
              .isEqualTo(URI.create("https://trimly.example.test"));
          assertThat(properties.ownHosts())
              .containsExactlyInAnyOrder("trimly.example.test", "www.trimly.example.test");
          assertThat(properties.shortUrlFor("abc2345"))
              .isEqualTo("https://trimly.example.test/abc2345");
        });
  }

  @Test
  void thePublicBaseUrlHostIsAlwaysAnOwnHost() {
    runner
        .withPropertyValues(
            "trimly.public-base-url=https://sho.example.test", "trimly.own-hosts=localhost")
        .run(context ->
            assertThat(context.getBean(LinkProperties.class).ownHosts())
                .containsExactlyInAnyOrder("sho.example.test", "localhost"));
  }

  @Test
  void productionConfigurationHasNoLocalhostDefaults() throws Exception {
    var main =
        new YamlPropertySourceLoader()
            .load("main", new ClassPathResource("application.yml"))
            .getFirst();

    assertThat(main.getProperty("trimly.public-base-url")).isNull();
    assertThat(main.getProperty("trimly.own-hosts[0]")).isNull();
  }

  @Test
  void refusesToStartWithoutAPublicBaseUrl() {
    runner
        .withPropertyValues("trimly.own-hosts=trimly.example.test")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void refusesToStartWithoutOwnHosts() {
    runner
        .withPropertyValues("trimly.public-base-url=https://trimly.example.test")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void refusesARelativeNonWebOrSlashTerminatedPublicBaseUrl() {
    for (String bad :
        new String[] {"/relative", "ftp://trimly.example.test", "https://trimly.example.test/"}) {
      runner
          .withPropertyValues(
              "trimly.public-base-url=" + bad, "trimly.own-hosts=trimly.example.test")
          .run(context -> assertThat(context).as(bad).hasFailed());
    }
  }

  @Test
  void refusesAPublicBaseUrlWithAPathQueryFragmentOrUserinfo() {
    for (String bad :
        new String[] {
          "https://trimly.example.test/s",
          "https://trimly.example.test?x=1",
          "https://trimly.example.test#f",
          "https://owner@trimly.example.test"
        }) {
      runner
          .withPropertyValues(
              "trimly.public-base-url=" + bad, "trimly.own-hosts=trimly.example.test")
          .run(context -> assertThat(context).as(bad).hasFailed());
    }
  }

  @Test
  void acceptsAPublicBaseUrlWithAPort() {
    runner
        .withPropertyValues(
            "trimly.public-base-url=http://localhost:8080", "trimly.own-hosts=localhost")
        .run(context ->
            assertThat(context.getBean(LinkProperties.class).shortUrlFor("abc2345"))
                .isEqualTo("http://localhost:8080/abc2345"));
  }

  @Test
  void refusesAnOwnHostThatIsNotABareHostName() {
    for (String bad :
        new String[] {
          "https://x.example.test", "x.example.test/", "x.example.test:443", "u@x.example.test"
        }) {
      runner
          .withPropertyValues(
              "trimly.public-base-url=https://trimly.example.test", "trimly.own-hosts=" + bad)
          .run(context -> assertThat(context).as(bad).hasFailed());
    }
  }

  @Test
  void acceptsAnIpv6LiteralOwnHost() {
    runner
        .withPropertyValues(
            "trimly.public-base-url=https://trimly.example.test", "trimly.own-hosts=[::1]")
        .run(context ->
            assertThat(context.getBean(LinkProperties.class).ownHosts()).contains("[::1]"));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(LinkProperties.class)
  static class Config {}
}
