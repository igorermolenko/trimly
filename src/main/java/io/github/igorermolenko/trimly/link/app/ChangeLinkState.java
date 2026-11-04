package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Sets a link's state with one idempotent statement: repeating a change confirms the current state
 * and changes nothing (AC-14). Only {@code state} and {@code updated_at} ever change.
 */
@Component
class ChangeLinkState {

  private final ShortLinkStore store;
  private final Clock clock;

  ChangeLinkState(ShortLinkStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  LinkStateView change(String rawCode, LinkState target) {
    if (!ShortCode.isWellFormed(rawCode)) {
      throw new LinkNotFound();
    }
    ShortCode code = ShortCode.of(rawCode);
    return StorageFailures.call(
            () -> store.setState(code, target, clock.instant().truncatedTo(ChronoUnit.MILLIS)))
        .map(link -> new LinkStateView(link.shortCode(), link.state()))
        .orElseThrow(LinkNotFound::new);
  }
}
