package io.github.igorermolenko.trimly.link.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected.Reason;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TargetUrlTest {

  private static final Set<String> OWN_HOSTS = Set.of("trimly.example.test");

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://example.com",
        "http://example.com/a?b=c#d",
        "HTTPS://Example.COM/Path%20With%20Space",
        "https://xn--e1afmkfd.xn--p1ai/",
        "https://nottrimly.example.test/",
        "https://example.com:8443/x",
        "https://[2001:db8::1]/x"
      })
  void acceptsAbsoluteHttpAndHttpsUrlsExactlyAsSubmitted(String raw) {
    assertThat(TargetUrl.parse(raw, OWN_HOSTS).value()).isEqualTo(raw);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "example.test/a",
        "javascript:alert(1)",
        "data:text/html,hi",
        "ftp://h/",
        "https:///path",
        "mailto:someone@example.com",
        "/relative/path",
        "https:example.com"
      })
  void rejectsAnythingButAnAbsoluteWebAddressAsUnsupported(String raw) {
    assertRejected(raw, Reason.UNSUPPORTED);
  }

  @Test
  void acceptsExactly2048CharactersAndRejects2049AsTooLong() {
    String prefix = "https://example.com/";
    String exactly2048 = prefix + "a".repeat(2048 - prefix.length());

    assertThat(TargetUrl.parse(exactly2048, OWN_HOSTS).value()).hasSize(2048);
    assertRejected(exactly2048 + "a", Reason.TOO_LONG);
  }

  @Test
  void countsLengthOnTheRawTextBeforeAnyOtherCheck() {
    assertRejected(" " + "a".repeat(2048), Reason.TOO_LONG);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        " https://example.com",
        "https://example.com ",
        "https://example.com/a b",
        "https://example.com/\n",
        "https://example.com/\ta",
        "https://example.com/\u007f",
        "https://example.com/café",
        "https://пример.рф/"
      })
  void rejectsSpacesControlAndNonAsciiCharactersAsNotEncoded(String raw) {
    assertRejected(raw, Reason.NOT_ENCODED);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://trimly.example.test/abc2345",
        "HTTPS://Trimly.Example.Test./x",
        "https://a.trimly.example.test/",
        "http://user@trimly.example.test:8080/"
      })
  void rejectsTrimlysOwnHostsAndTheirSubdomains(String raw) {
    assertRejected(raw, Reason.POINTS_TO_TRIMLY);
  }

  @Test
  void ignoresATrailingDotAndCaseInTheConfiguredHost() {
    assertThat(
            catchThrowableOfType(
                    TargetUrlRejected.class,
                    () ->
                        TargetUrl.parse(
                            "https://trimly.example.test/", Set.of("Trimly.Example.Test.")))
                .reason())
        .isEqualTo(Reason.POINTS_TO_TRIMLY);
  }

  private static void assertRejected(String raw, Reason reason) {
    TargetUrlRejected rejected =
        catchThrowableOfType(TargetUrlRejected.class, () -> TargetUrl.parse(raw, OWN_HOSTS));
    assertThat(rejected).as(String.valueOf(raw)).isNotNull();
    assertThat(rejected.reason()).as(String.valueOf(raw)).isEqualTo(reason);
  }
}
