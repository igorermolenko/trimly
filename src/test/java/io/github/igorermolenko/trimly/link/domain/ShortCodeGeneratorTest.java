package io.github.igorermolenko.trimly.link.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

  @Test
  void generatesWellFormedCodes() {
    ShortCodeGenerator generator = ShortCodeGenerator.from(new SecureRandom());

    Set<Character> seen = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      String code = generator.next().value();
      assertThat(ShortCode.isWellFormed(code)).as(code).isTrue();
      code.chars().forEach(c -> seen.add((char) c));
    }
    assertThat(seen).hasSize(ShortCode.ALPHABET.length());
  }

  @Test
  void aSeededGeneratorIsDeterministic() {
    ShortCodeGenerator first = ShortCodeGenerator.from(new Random(42));
    ShortCodeGenerator second = ShortCodeGenerator.from(new Random(42));

    IntStream.range(0, 100).forEach(i -> assertThat(first.next()).isEqualTo(second.next()));
  }

  @Test
  void consecutiveCodesDiffer() {
    ShortCodeGenerator generator = ShortCodeGenerator.from(new SecureRandom());

    assertThat(generator.next()).isNotEqualTo(generator.next());
  }
}
