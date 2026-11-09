package io.github.igorermolenko.trimly.redirect.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * the shared Problem Details advice answers it like any other malformed owner request. The firewall
 * runs before Spring Security's header writers, so the owner-API no-cache and nosniff headers are
 * written here. Addresses Tomcat rejects before the firewall runs are answered the same way by
 * {@link ServerRejectedAddressValve}.
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
    if (RejectedAddressAnswers.isOwnerApi(request)) {
      RejectedAddressAnswers.writeOwnerApiHeaders(request, response);
      if (ownerApi.resolveException(request, response, null, rejected) == null) {
        fallback.handle(request, response, rejected);
      }
      return;
    }
    RejectedAddressAnswers.writeNotFound(response);
  }
}
