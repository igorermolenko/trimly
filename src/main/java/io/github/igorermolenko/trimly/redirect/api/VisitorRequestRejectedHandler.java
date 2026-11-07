package io.github.igorermolenko.trimly.redirect.api;

import io.github.igorermolenko.trimly.redirect.app.VisitorAnswer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Spring Security's firewall rejects some addresses (a {@code ;}, an encoded {@code %} or {@code
 * /}, an empty segment) before {@link RedirectController} sees them. Such an address can never be a
 * short code, so a visitor gets the same plain-text "link not found" answer as for any other
 * malformed code (AC-16b). An owner-API address is handed to Spring MVC's exception resolvers, so
 * the shared Problem Details advice answers it like any other malformed owner request.
 */
@Component
class VisitorRequestRejectedHandler implements RequestRejectedHandler {

  private final HandlerExceptionResolver ownerApi;
  private final RequestRejectedHandler fallback = new HttpStatusRequestRejectedHandler();

  VisitorRequestRejectedHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver ownerApi) {
    this.ownerApi = ownerApi;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, RequestRejectedException rejected)
      throws IOException, ServletException {
    String path = request.getRequestURI();
    if (path.equals("/api") || path.startsWith("/api/") || path.startsWith("/api;")) {
      if (ownerApi.resolveException(request, response, null, rejected) == null) {
        fallback.handle(request, response, rejected);
      }
      return;
    }
    VisitorAnswer.Message notFound = VisitorAnswer.Message.NOT_FOUND;
    response.setStatus(notFound.status());
    response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    response.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8).toString());
    response.getWriter().write(notFound.text());
  }
}
