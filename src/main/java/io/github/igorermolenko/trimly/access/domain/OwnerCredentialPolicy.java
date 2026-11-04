package io.github.igorermolenko.trimly.access.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule a randomly generated owner credential must meet, since trimly cannot check randomness
 * itself (spec §6.1): at least {@value #MIN_LENGTH} characters, printable ASCII with no spaces, and
 * at least {@value #MIN_DISTINCT} distinct characters. Violation messages never contain the
 * credential.
 */
public final class OwnerCredentialPolicy {

  public static final int MIN_LENGTH = 32;
  public static final int MIN_DISTINCT = 16;

  private OwnerCredentialPolicy() {}

  /** The rules {@code candidate} breaks; empty when it is acceptable. */
  public static List<String> check(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return List.of("is missing");
    }
    List<String> violations = new ArrayList<>();
    if (candidate.length() < MIN_LENGTH) {
      violations.add("is shorter than " + MIN_LENGTH + " characters");
    }
    if (!candidate.chars().allMatch(c -> c >= 0x21 && c <= 0x7E)) {
      violations.add("contains a space, a control character or a non-ASCII character");
    }
    if (candidate.chars().distinct().count() < MIN_DISTINCT) {
      violations.add("has fewer than " + MIN_DISTINCT + " distinct characters");
    }
    return List.copyOf(violations);
  }
}
