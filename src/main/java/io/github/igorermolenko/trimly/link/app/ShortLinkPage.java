package io.github.igorermolenko.trimly.link.app;

import java.util.List;

/**
 * One page of the link list, newest first.
 *
 * @param nextCursor the token that continues right after the last item; null when {@code hasNext}
 *     is false
 */
public record ShortLinkPage(List<ShortLinkView> items, boolean hasNext, String nextCursor) {}
