package io.github.igorermolenko.trimly.link.domain;

/**
 * The public, case-sensitive code of a short link: exactly {@value #LENGTH} characters from {@link
 * #ALPHABET}. This is the single definition of the alphabet and of a well-formed code (sad §4).
 */
public record ShortCode(String value) {

  /** A–Z, a–z and 2–9 without the look-alikes 0, O, o, 1, l and I: 56 characters. */
  public static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

  public static final int LENGTH = 7;

  public ShortCode {
    if (!isWellFormed(value)) {
      throw new IllegalArgumentException("Not a well-formed short code");
    }
  }

  public static ShortCode of(String raw) {
    return new ShortCode(raw);
  }

  /** Whether {@code raw} has the shape of a short code; never throws. */
  public static boolean isWellFormed(String raw) {
    if (raw == null || raw.length() != LENGTH) {
      return false;
    }
    return raw.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
  }

  @Override
  public String toString() {
    return value;
  }
}
