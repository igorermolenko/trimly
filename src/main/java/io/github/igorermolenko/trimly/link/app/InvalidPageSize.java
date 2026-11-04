package io.github.igorermolenko.trimly.link.app;

/** A page size outside 1..{@value ListShortLinks#MAX_PAGE_SIZE}. */
public final class InvalidPageSize extends RuntimeException {

  InvalidPageSize() {
    super("Invalid page size", null, false, false);
  }
}
