package io.github.igorermolenko.trimly.access.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers "authentication required" as Problem Details. The filter chain rejects the request before
 * any controller runs, so the answer is handed to the MVC exception resolvers: it is rendered by
 * the same machinery, and has the same shape, as every other error (sad §8).
 */
final class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

  static final String CODE = "access.authentication_required";
  static final String DETAIL = "Authentication is required.";

  private final HandlerExceptionResolver resolver;

  ProblemDetailsAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETAIL);
    problem.setProperty("code", CODE);
    var error = new ErrorResponseException(HttpStatus.UNAUTHORIZED, problem, null);
    error.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    resolver.resolveException(request, response, null, error);
  }
}
