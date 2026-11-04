package io.github.igorermolenko.trimly.link.domain;

/** The short code chosen for a new link is already held by another link (ADR-0002). */
public final class ShortCodeTaken extends RuntimeException {

  public ShortCodeTaken() {
    super("Short code already taken", null, false, false);
  }
}
