package io.github.igorermolenko.trimly.redirect.api;

import io.github.igorermolenko.trimly.redirect.app.VisitorAnswer;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Takes {@code /error} over from Boot's {@code BasicErrorController}. A visitor who opens {@code
 * /error} directly is asking for a code of the wrong length, so they get the plain-text "link not
 * found" (AC-16b), not Boot's error JSON. A real error dispatch (a {@code sendError} somewhere in
 * the chain) is handed to the shared Problem Details advice with its status, so it gets a {@code
 * code} like every other error.
 */
@RestController
class VisitorErrorController implements ErrorController {

  @RequestMapping("/error")
  ResponseEntity<String> error(HttpServletRequest request) {
    if (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer status) {
      throw new ErrorResponseException(HttpStatusCode.valueOf(status));
    }
    return RedirectController.answer(VisitorAnswer.Message.NOT_FOUND);
  }
}
