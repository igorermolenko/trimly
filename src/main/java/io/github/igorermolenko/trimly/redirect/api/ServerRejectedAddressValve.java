package io.github.igorermolenko.trimly.redirect.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.http.MediaType;

/**
 * Tomcat rejects some addresses itself while decoding or normalising them: an invalid
 * percent-escape ({@code %zz}, a lone {@code %}), an encoded NUL or backslash, a path that climbs
 * above the root. That happens before any filter, the Spring Security firewall or the {@code
 * /error} page runs, so Tomcat's error-report valve would answer with its own HTML 400. This valve
 * takes its place on the host: a visitor gets the plain-text "link not found" (AC-16b), and an
 * owner-API address gets the shared {@code shared.malformed_request} Problem. Every other error
 * keeps Tomcat's default answer, without a report or server info.
 */
class ServerRejectedAddressValve extends ErrorReportValve {

  /** The shared advice's answer for a rejected owner-API address, written without Spring MVC. */
  static final String MALFORMED_REQUEST_PROBLEM =
      """
      {"type":"about:blank","title":"Bad Request","status":400,\
      "detail":"The request address is not valid.","code":"shared.malformed_request"}\
      """;

  ServerRejectedAddressValve() {
    setShowReport(false);
    setShowServerInfo(false);
  }

  @Override
  protected void report(Request request, Response response, Throwable throwable) {
    // trimly's own 400s always carry a body; a body-less 400 is one Tomcat raised itself.
    if (response.getStatus() != 400 || throwable != null || response.getContentWritten() > 0) {
      super.report(request, response, throwable);
      return;
    }
    if (!response.setErrorReported()) {
      return;
    }
    try {
      if (RejectedAddressAnswers.isOwnerApi(request)) {
        RejectedAddressAnswers.writeOwnerApiHeaders(request, response);
        response.setContentType(
            new MediaType(MediaType.APPLICATION_PROBLEM_JSON, StandardCharsets.UTF_8).toString());
        response.getWriter().write(MALFORMED_REQUEST_PROBLEM);
      } else {
        RejectedAddressAnswers.writeNotFound(response);
      }
      response.finishResponse();
    } catch (IOException | IllegalStateException e) {
      // The client went away or the response is already gone; there is no one left to answer.
    }
  }
}
