package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import java.time.Instant;

/** A short link as the owner sees it, with its full short URL. */
public record ShortLinkView(
    ShortCode shortCode, String shortUrl, String targetUrl, LinkState state, Instant createdAt) {

  static ShortLinkView of(ShortLink link, LinkProperties properties) {
    return new ShortLinkView(
        link.shortCode(),
        properties.shortUrlFor(link.shortCode().value()),
        link.targetUrl(),
        link.state(),
        link.createdAt());
  }
}
