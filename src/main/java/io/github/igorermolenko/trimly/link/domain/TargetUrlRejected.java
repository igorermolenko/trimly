package io.github.igorermolenko.trimly.link.domain;

/** A submitted target URL that cannot become a short link, with the rule it broke. */
public final class TargetUrlRejected extends RuntimeException {

  /** Which rule the target URL broke. */
  public enum Reason {
    /** Missing, or not an absolute http/https address with a host (AC-03). */
    UNSUPPORTED,
    /** Longer than {@value TargetUrl#MAX_LENGTH} characters as submitted (AC-04). */
    TOO_LONG,
    /** Contains a space, a control character or a non-ASCII character (AC-05). */
    NOT_ENCODED,
    /** Its host is one of trimly's own hosts or a subdomain of one (AC-06). */
    POINTS_TO_TRIMLY
  }

  private final Reason reason;

  public TargetUrlRejected(Reason reason) {
    super("Target URL rejected: " + reason, null, false, false);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
