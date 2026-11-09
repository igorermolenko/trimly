package io.github.igorermolenko.trimly.link.app;

/** A continuation token this API did not issue. */
public final class InvalidCursor extends RuntimeException {

  InvalidCursor() {
    super("Invalid continuation token", null, false, false);
  }
}
