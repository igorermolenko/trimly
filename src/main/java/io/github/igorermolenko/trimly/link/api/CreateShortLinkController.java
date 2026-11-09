package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.app.CreateShortLink;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/links}: the owner creates a short link (AC-01…AC-06, AC-17b). */
@RestController
class CreateShortLinkController {

  /** The {@code ShortLinkCreate} body; a missing {@code targetUrl} is an unsupported one. */
  record ShortLinkCreate(String targetUrl) {}

  private final CreateShortLink createShortLink;

  CreateShortLinkController(CreateShortLink createShortLink) {
    this.createShortLink = createShortLink;
  }

  @PostMapping("/api/v1/links")
  @ResponseStatus(HttpStatus.CREATED)
  ShortLinkResponse create(@RequestBody ShortLinkCreate body) {
    try {
      return ShortLinkResponse.of(createShortLink.create(body.targetUrl()));
    } catch (TargetUrlRejected e) {
      throw LinkProblems.of(e.reason()).exception();
    } catch (LinkStorageUnavailable e) {
      throw LinkProblems.TEMPORARILY_UNAVAILABLE.exception();
    }
  }
}
