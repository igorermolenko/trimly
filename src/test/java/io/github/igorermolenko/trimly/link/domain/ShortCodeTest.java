package io.github.igorermolenko.trimly.link.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ShortCodeTest {

  @Test
  void alphabetHasExactly56DistinctCharacters() {
    assertThat(ShortCode.ALPHABET).hasSize(56);
    assertThat(ShortCode.ALPHABET.chars().distinct().count()).isEqualTo(56);
  }

  @Test
  void alphabetIsLettersAndDigitsTwoToNineWithoutTheSixLookAlikes() {
    String expected =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz23456789"
            .chars()
            .filter(c -> "0Oo1lI".indexOf(c) < 0)
            .mapToObj(c -> String.valueOf((char) c))
            .collect(Collectors.joining());

    assertThat(ShortCode.ALPHABET.chars().sorted().toArray())
        .containsExactly(expected.chars().sorted().toArray());
    assertThat(ShortCode.ALPHABET).doesNotContain("0", "O", "o", "1", "l", "I");
  }

  @Test
  void lengthIsSeven() {
    assertThat(ShortCode.LENGTH).isEqualTo(7);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc2345", "ABC2345", "zzzzzzz", "2345678", "HJKMNPQ"})
  void acceptsSevenAlphabetCharacters(String raw) {
    assertThat(ShortCode.isWellFormed(raw)).isTrue();
    assertThat(ShortCode.of(raw).value()).isEqualTo(raw);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "abc2345.",
        "abc234",
        "abc23456",
        "abc 234",
        "abc0345",
        "abcO345",
        "abco345",
        "abc1345",
        "abcl345",
        "abcI345",
        "abc-345",
        "abc/345",
        "abc2345/",
        "abcé345"
      })
  void rejectsAnythingElse(String raw) {
    assertThat(ShortCode.isWellFormed(raw)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> ShortCode.of(raw));
  }

  @Test
  void letterCaseMatters() {
    assertThat(ShortCode.of("abc2345")).isNotEqualTo(ShortCode.of("ABC2345"));
  }
}
