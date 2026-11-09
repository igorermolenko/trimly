package io.github.igorermolenko.trimly.link.domain;

import java.util.random.RandomGenerator;

/**
 * Draws fresh short codes. Production uses a {@link java.security.SecureRandom}, so a code cannot
 * be predicted from earlier ones (AC-01); tests can supply their own codes to force collisions
 * (ADR-0002).
 */
@FunctionalInterface
public interface ShortCodeGenerator {

  ShortCode next();

  /** Each character is drawn uniformly from {@link ShortCode#ALPHABET} using {@code random}. */
  static ShortCodeGenerator from(RandomGenerator random) {
    return () -> {
      char[] chars = new char[ShortCode.LENGTH];
      for (int i = 0; i < chars.length; i++) {
        chars[i] = ShortCode.ALPHABET.charAt(random.nextInt(ShortCode.ALPHABET.length()));
      }
      return new ShortCode(new String(chars));
    };
  }
}
