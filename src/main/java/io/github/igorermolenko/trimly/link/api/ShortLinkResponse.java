package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.app.ShortLinkView;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import java.time.Instant;

/** The {@code ShortLink} schema of the owner API. */
record ShortLinkResponse(
    String shortCode, String shortUrl, String targetUrl, LinkState state, Instant createdAt) {

  static ShortLinkResponse of(ShortLinkView view) {
    return new ShortLinkResponse(
        view.shortCode().value(),
        view.shortUrl(),
        view.targetUrl(),
        view.state(),
        view.createdAt());
  }
}
