package io.github.igorermolenko.trimly.redirect.app;

/** What a visitor who opens a short link gets back. */
public sealed interface VisitorAnswer {

  /** A temporary, non-reusable redirect to {@code targetUrl} exactly as stored (AC-07). */
  record Redirect(String targetUrl) implements VisitorAnswer {}

  /** A short plain-text sentence (AC-16, AC-17). */
  enum Message implements VisitorAnswer {
    NOT_FOUND(404, "Link not found."),
    UNAVAILABLE(503, "Temporarily unavailable, try again later.");

    private final int status;
    private final String text;

    Message(int status, String text) {
      this.status = status;
      this.text = text;
    }

    public int status() {
      return status;
    }

    public String text() {
      return text;
    }
  }
}
