package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.app.ShortLinkPage;
import java.util.List;

/** The {@code ShortLinkPage} schema; {@code nextCursor} is serialised as null on the last page. */
record ShortLinkPageResponse(List<ShortLinkResponse> items, boolean hasNext, String nextCursor) {

  static ShortLinkPageResponse of(ShortLinkPage page) {
    return new ShortLinkPageResponse(
        page.items().stream().map(ShortLinkResponse::of).toList(),
        page.hasNext(),
        page.nextCursor());
  }
}
