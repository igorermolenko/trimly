package io.github.igorermolenko.trimly.access.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OwnerCredentialPolicyTest {

  static final String VALID = "Zq3!vR8#mK2$wT6%yH9&nB4*pL7(xD5)";

  @Test
  void acceptsThirtyTwoPrintableCharactersWithSixteenDistinct() {
    assertThat(VALID).hasSize(32);
    assertThat(OwnerCredentialPolicy.check(VALID)).isEmpty();
  }

  @Test
  void acceptsExactlySixteenDistinctCharacters() {
    String sixteenDistinct = "abcdefghijklmnop".repeat(2);

    assertThat(OwnerCredentialPolicy.check(sixteenDistinct)).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void rejectsAMissingOrBlankCredential(String candidate) {
    assertThat(OwnerCredentialPolicy.check(candidate)).isNotEmpty();
  }

  @Test
  void rejectsThirtyOneCharacters() {
    assertThat(OwnerCredentialPolicy.check(VALID.substring(1))).isNotEmpty();
  }

  @Test
  void rejectsFifteenDistinctCharacters() {
    String fifteenDistinct = "abcdefghijklmno".repeat(3);

    assertThat(fifteenDistinct.length()).isGreaterThanOrEqualTo(32);
    assertThat(OwnerCredentialPolicy.check(fifteenDistinct)).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "é", "\n", "\u007f"})
  void rejectsSpacesControlAndNonAsciiCharacters(String bad) {
    assertThat(OwnerCredentialPolicy.check(VALID + bad)).isNotEmpty();
  }

  @Test
  void violationsNeverContainTheCredential() {
    String weak = "secret-value-that-is-too-short";

    List<String> violations = OwnerCredentialPolicy.check(weak);

    assertThat(violations).isNotEmpty().noneMatch(v -> v.contains(weak) || v.contains("secret"));
  }
}
