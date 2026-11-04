package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The owner-API errors of the {@code link} module: each a fixed {@code code} and {@code detail},
 * rendered as Problem Details by the shared advice. Never carries an exception message.
 */
enum LinkProblems {
  TARGET_URL_UNSUPPORTED(
      HttpStatus.BAD_REQUEST,
      "link.target_url_unsupported",
      "Only absolute http or https web addresses are accepted."),
  TARGET_URL_TOO_LONG(
      HttpStatus.BAD_REQUEST,
      "link.target_url_too_long",
      "The target URL must be at most 2048 characters long."),
  TARGET_URL_NOT_ENCODED(
      HttpStatus.BAD_REQUEST,
      "link.target_url_not_encoded",
      "The target URL must be properly encoded: non-Latin hosts in their ASCII xn-- form, other"
          + " characters percent-encoded."),
  TARGET_URL_POINTS_TO_TRIMLY(
      HttpStatus.BAD_REQUEST,
      "link.target_url_points_to_trimly",
      "A short link cannot point back to trimly."),
  INVALID_PAGE_SIZE(
      HttpStatus.BAD_REQUEST,
      "link.invalid_page_size",
      "The page size must be a whole number from 1 to 100."),
  INVALID_CURSOR(
      HttpStatus.BAD_REQUEST,
      "link.invalid_cursor",
      "The continuation token is not valid. Start again from the first page."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "link.not_found", "The link was not found."),
  TEMPORARILY_UNAVAILABLE(
      HttpStatus.SERVICE_UNAVAILABLE,
      "link.temporarily_unavailable",
      "Temporarily unavailable, try again later.");

  private final HttpStatus status;
  private final String code;
  private final String detail;

  LinkProblems(HttpStatus status, String code, String detail) {
    this.status = status;
    this.code = code;
    this.detail = detail;
  }

  ErrorResponseException exception() {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("code", code);
    return new ErrorResponseException(status, problem, null);
  }

  static LinkProblems of(TargetUrlRejected.Reason reason) {
    return switch (reason) {
      case UNSUPPORTED -> TARGET_URL_UNSUPPORTED;
      case TOO_LONG -> TARGET_URL_TOO_LONG;
      case NOT_ENCODED -> TARGET_URL_NOT_ENCODED;
      case POINTS_TO_TRIMLY -> TARGET_URL_POINTS_TO_TRIMLY;
    };
  }
}
