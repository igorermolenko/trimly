package io.github.igorermolenko.trimly.redirect.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.ErrorResponseException;

class VisitorErrorControllerTest {

  private final VisitorErrorController controller = new VisitorErrorController();

  @Test
  void aRealErrorDispatchIsHandedToTheProblemDetailsAdviceWithItsStatus() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);

    ErrorResponseException thrown =
        catchThrowableOfType(ErrorResponseException.class, () -> controller.error(request));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void aDirectRequestIsTheVisitorsLinkNotFound() {
    var response = controller.error(new MockHttpServletRequest("GET", "/error"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isEqualTo("Link not found.");
    assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
  }
}
