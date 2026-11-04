package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Lists the owner's links newest first, a page at a time, by keyset on the monotonic id: links
 * created while paging never duplicate or skip an item (AC-09, ADR-0004).
 */
@Service
public class ListShortLinks {

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

  private final ShortLinkStore store;
  private final LinkProperties properties;

  ListShortLinks(ShortLinkStore store, LinkProperties properties) {
    this.store = store;
    this.properties = properties;
  }

  /**
   * @param after a token from an earlier page, or null for the first page
   * @param limit the page size, or null for {@value #DEFAULT_PAGE_SIZE}
   * @throws InvalidCursor when {@code after} is not a token this API issued
   * @throws InvalidPageSize when {@code limit} is outside 1..{@value #MAX_PAGE_SIZE}
   * @throws LinkStorageUnavailable when storage does not answer in time
   */
  public ShortLinkPage list(String after, Integer limit) {
    int size = limit == null ? DEFAULT_PAGE_SIZE : limit;
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new InvalidPageSize();
    }
    UUID cursor = after == null ? null : PageCursor.decode(after);

    List<ShortLink> rows = StorageFailures.call(() -> store.page(cursor, size));
    boolean hasNext = rows.size() > size;
    List<ShortLink> items = hasNext ? rows.subList(0, size) : rows;
    return new ShortLinkPage(
        items.stream().map(link -> ShortLinkView.of(link, properties)).toList(),
        hasNext,
        hasNext ? PageCursor.encode(items.getLast().id()) : null);
  }
}
