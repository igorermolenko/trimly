package io.github.igorermolenko.trimly.redirect.api;

import io.github.igorermolenko.trimly.redirect.app.VisitorAnswer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;

/**
 * The answers for an address rejected before trimly's controllers run, whether Spring Security's
 * firewall or Tomcat itself rejected it: a visitor gets the plain-text "link not found" (AC-16b);
 * an owner-API address keeps the owner-API no-cache and nosniff headers, which Spring Security
 * would otherwise write later in the chain.
 */
final class RejectedAddressAnswers {

  private static final List<HeaderWriter> OWNER_API_HEADERS =
      List.of(new CacheControlHeadersWriter(), new XContentTypeOptionsHeaderWriter());

  private RejectedAddressAnswers() {}

  static boolean isOwnerApi(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.equals("/api") || path.startsWith("/api/") || path.startsWith("/api;");
  }

  static void writeOwnerApiHeaders(HttpServletRequest request, HttpServletResponse response) {
    OWNER_API_HEADERS.forEach(writer -> writer.writeHeaders(request, response));
  }

  static void writeNotFound(HttpServletResponse response) throws IOException {
    VisitorAnswer.Message notFound = VisitorAnswer.Message.NOT_FOUND;
    response.setStatus(notFound.status());
    response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    response.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8).toString());
    response.getWriter().write(notFound.text());
  }
}
