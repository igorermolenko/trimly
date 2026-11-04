package io.github.igorermolenko.trimly.access.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class OwnerCredentialPropertiesTest {

  private static final String VALID = "Zq3!vR8#mK2$wT6%yH9&nB4*pL7(xD5)";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(Config.class);

  @Test
  void startsWithAValidCredential() {
    runner
        .withPropertyValues("trimly.owner-credential=" + VALID)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(OwnerCredentialProperties.class).ownerCredential())
              .isEqualTo(VALID);
        });
  }

  @Test
  void refusesToStartWithoutACredential() {
    runner.run(context ->
        assertThat(context)
            .hasFailed()
            .getFailure()
            .hasStackTraceContaining("trimly.owner-credential"));
  }

  @Test
  void refusesToStartWithAWeakCredentialWithoutRevealingIt() {
    String weak = "tooshort-but-printable";

    runner
        .withPropertyValues("trimly.owner-credential=" + weak)
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(stackTrace(context.getStartupFailure())).doesNotContain(weak);
        });
  }

  @Test
  void toStringRedactsTheCredential() {
    assertThat(new OwnerCredentialProperties(VALID).toString()).doesNotContain(VALID);
  }

  private static String stackTrace(Throwable failure) {
    var out = new java.io.StringWriter();
    failure.printStackTrace(new java.io.PrintWriter(out));
    return out.toString();
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(OwnerCredentialProperties.class)
  static class Config {}
}
