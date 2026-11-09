package io.github.igorermolenko.trimly.link.domain;

/** No link has the given short code, or the code cannot be one (AC-13). */
public final class LinkNotFound extends RuntimeException {

  public LinkNotFound() {
    super("Link not found", null, false, false);
  }
}
