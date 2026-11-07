package io.github.igorermolenko.trimly.redirect.api;

import io.github.igorermolenko.trimly.redirect.app.FollowShortLink;
import io.github.igorermolenko.trimly.redirect.app.VisitorAnswer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /{shortCode}} for visitors, with no credential. One path segment and an optional
 * single trailing slash; the query string is ignored. Every answer is built here with {@code
 * Cache-Control: no-store}, never thrown to the shared advice.
 */
@RestController
class RedirectController {

  private static final MediaType TEXT =
      new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8);

  private final FollowShortLink followShortLink;

  RedirectController(FollowShortLink followShortLink) {
    this.followShortLink = followShortLink;
  }

  @GetMapping({"/{segment}", "/{segment}/"})
  ResponseEntity<String> follow(@PathVariable String segment) {
    return answer(followShortLink.follow(segment));
  }

  /** The bare root is a code of the wrong length (empty): "link not found" without a lookup. */
  @GetMapping("/")
  ResponseEntity<String> root() {
    return answer(VisitorAnswer.Message.NOT_FOUND);
  }

  /** More than one segment can never be a short code: "link not found" without a lookup. */
  @GetMapping("/{first}/{second}/{*rest}")
  ResponseEntity<String> tooManySegments(@PathVariable String first) {
    if ("api".equals(first)) {
      // An unknown owner-API path stays a Problem Details 404.
      throw new ErrorResponseException(HttpStatus.NOT_FOUND);
    }
    return answer(VisitorAnswer.Message.NOT_FOUND);
  }

  static ResponseEntity<String> answer(VisitorAnswer answer) {
    return switch (answer) {
      case VisitorAnswer.Redirect redirect ->
          ResponseEntity.status(HttpStatus.FOUND)
              .cacheControl(CacheControl.noStore())
              .header(HttpHeaders.LOCATION, redirect.targetUrl())
              .build();
      case VisitorAnswer.Message message ->
          ResponseEntity.status(message.status())
              .cacheControl(CacheControl.noStore())
              .contentType(TEXT)
              .body(message.text());
    };
  }
}
