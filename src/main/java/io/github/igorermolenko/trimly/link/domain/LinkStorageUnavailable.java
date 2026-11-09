package io.github.igorermolenko.trimly.link.domain;

/** The link storage did not answer in time or could not be reached (ADR-0003, AC-17, AC-17b). */
public final class LinkStorageUnavailable extends RuntimeException {

  public LinkStorageUnavailable(Throwable cause) {
    super("Link storage unavailable", cause);
  }
}
