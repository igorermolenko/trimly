package io.github.igorermolenko.trimly.shared;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * The one place that turns exceptions into RFC 9457 Problem Details responses. Spring MVC's own
 * exceptions are handled by {@link ResponseEntityExceptionHandler}; anything unexpected becomes a
 * generic 500 that never exposes the exception message or stack trace. Every answer carries a
 * {@code type} ({@code about:blank} unless a module set one) and a {@code code} (the contract's
 * {@code Problem.required}): a module's own code when it set one, else a {@code shared.*} code for
 * the status.
 */
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

  static final String CODE = "code";
  static final URI ABOUT_BLANK = URI.create("about:blank");

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailsAdvice.class);
  private static final String MALFORMED_DETAIL =
      "The request body is not valid JSON of the expected shape.";

  /** A body that is not the JSON the endpoint expects: {@code code: shared.malformed_request}. */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return malformed(ex, headers, request);
  }

  /** A body that is not JSON at all (wrong or missing Content-Type) is malformed too, not a 415. */
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return malformed(ex, new HttpHeaders(), request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problem.setType(ABOUT_BLANK);
    problem.setProperty(CODE, "shared.internal_error");
    return problem;
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(
      Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      if (problem.getType() == null) {
        problem.setType(ABOUT_BLANK);
      }
      if (problem.getProperties() == null || !problem.getProperties().containsKey(CODE)) {
        problem.setProperty(CODE, fallbackCode(status));
      }
    }
    return super.createResponseEntity(body, headers, status, request);
  }

  private ResponseEntity<Object> malformed(Exception ex, HttpHeaders headers, WebRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, MALFORMED_DETAIL);
    problem.setProperty(CODE, "shared.malformed_request");
    return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  private static String fallbackCode(HttpStatusCode status) {
    return switch (status.value()) {
      case 404 -> "shared.not_found";
      case 405 -> "shared.method_not_allowed";
      case 406 -> "shared.not_acceptable";
      default -> status.is5xxServerError() ? "shared.internal_error" : "shared.malformed_request";
    };
  }
}
