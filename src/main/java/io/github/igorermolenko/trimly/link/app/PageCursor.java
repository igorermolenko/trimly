package io.github.igorermolenko.trimly.link.app;

import java.util.UUID;

/**
 * The continuation token of the link list: the last listed link's id in canonical form (ADR-0004).
 * Opaque to clients; only tokens of that exact shape are accepted back.
 */
final class PageCursor {

  private PageCursor() {}

  static String encode(UUID lastId) {
    return lastId.toString();
  }

  static UUID decode(String token) {
    if (token == null || token.length() != 36) {
      throw new InvalidCursor();
    }
    UUID id;
    try {
      id = UUID.fromString(token);
    } catch (IllegalArgumentException e) {
      throw new InvalidCursor();
    }
    if (id.version() != 7 || id.variant() != 2 || !id.toString().equals(token)) {
      throw new InvalidCursor();
    }
    return id;
  }
}
