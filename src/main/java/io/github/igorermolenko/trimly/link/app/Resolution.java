package io.github.igorermolenko.trimly.link.app;

/**
 * The outcome of looking up a short link for a visitor (ADR-0005). An exhaustive {@code switch}
 * forces callers to handle all three.
 */
public sealed interface Resolution {

  /** An active link: send the visitor to {@code targetUrl}, exactly as stored. */
  record Found(String targetUrl) implements Resolution {}

  /** Unknown, disabled or malformed — deliberately indistinguishable (AC-12, AC-16). */
  enum NotFound implements Resolution {
    INSTANCE
  }

  /** Link storage did not answer within its budget (AC-17). */
  enum Unavailable implements Resolution {
    INSTANCE
  }
}
