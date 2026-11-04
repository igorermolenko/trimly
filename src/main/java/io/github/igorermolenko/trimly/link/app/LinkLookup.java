package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import org.springframework.stereotype.Service;

/**
 * The one contract {@code redirect} uses: resolves a raw path segment to a {@link Resolution}. A
 * segment that cannot be a short code never reaches storage (AC-16b), and every other lookup reads
 * storage afresh — nothing is cached (AC-17).
 */
@Service
public class LinkLookup {

  private final ShortLinkStore store;

  LinkLookup(ShortLinkStore store) {
    this.store = store;
  }

  public Resolution resolve(String rawSegment) {
    if (!ShortCode.isWellFormed(rawSegment)) {
      return Resolution.NotFound.INSTANCE;
    }
    ShortCode code = ShortCode.of(rawSegment);
    try {
      return StorageFailures.call(() -> store.findByShortCode(code))
          .filter(link -> link.state() == LinkState.ACTIVE)
          .<Resolution>map(link -> new Resolution.Found(link.targetUrl()))
          .orElse(Resolution.NotFound.INSTANCE);
    } catch (LinkStorageUnavailable e) {
      return Resolution.Unavailable.INSTANCE;
    }
  }
}
