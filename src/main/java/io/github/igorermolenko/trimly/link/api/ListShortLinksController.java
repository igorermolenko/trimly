package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.app.InvalidCursor;
import io.github.igorermolenko.trimly.link.app.InvalidPageSize;
import io.github.igorermolenko.trimly.link.app.ListShortLinks;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/links}: the owner's links, newest first, a page at a time (AC-09, AC-10). */
@RestController
class ListShortLinksController {

  private final ListShortLinks listShortLinks;

  ListShortLinksController(ListShortLinks listShortLinks) {
    this.listShortLinks = listShortLinks;
  }

  /** {@code limit} is bound as text so that {@code abc} is an invalid page size too. */
  @GetMapping("/api/v1/links")
  ShortLinkPageResponse list(
      @RequestParam(required = false) String after, @RequestParam(required = false) String limit) {
    try {
      return ShortLinkPageResponse.of(listShortLinks.list(after, pageSize(limit)));
    } catch (InvalidPageSize e) {
      throw LinkProblems.INVALID_PAGE_SIZE.exception();
    } catch (InvalidCursor e) {
      throw LinkProblems.INVALID_CURSOR.exception();
    } catch (LinkStorageUnavailable e) {
      throw LinkProblems.TEMPORARILY_UNAVAILABLE.exception();
    }
  }

  private static Integer pageSize(String limit) {
    if (limit == null) {
      return null;
    }
    try {
      return Integer.valueOf(limit);
    } catch (NumberFormatException e) {
      throw LinkProblems.INVALID_PAGE_SIZE.exception();
    }
  }
}
