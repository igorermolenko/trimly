package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import io.github.igorermolenko.trimly.link.domain.ShortCodeTaken;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.domain.TargetUrl;
import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import io.github.igorermolenko.trimly.shared.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Creates a new active short link for a target URL. Every creation issues a new link (AC-02). A
 * taken code is retried with a fresh one, each attempt its own statement with no surrounding
 * transaction (ADR-0002).
 */
@Service
public class CreateShortLink {

  static final int MAX_ATTEMPTS = 5;

  private final ShortLinkStore store;
  private final ShortCodeGenerator codes;
  private final IdGenerator ids;
  private final Clock clock;
  private final LinkProperties properties;

  CreateShortLink(
      ShortLinkStore store,
      ShortCodeGenerator codes,
      IdGenerator ids,
      Clock clock,
      LinkProperties properties) {
    this.store = store;
    this.codes = codes;
    this.ids = ids;
    this.clock = clock;
    this.properties = properties;
  }

  /**
   * @throws TargetUrlRejected when the target URL breaks a rule; nothing is stored
   * @throws LinkStorageUnavailable when storage does not answer in time, or {@value #MAX_ATTEMPTS}
   *     generated codes are all taken
   */
  public ShortLinkView create(String rawTargetUrl) {
    TargetUrl target = TargetUrl.parse(rawTargetUrl, properties.ownHosts());
    // Millisecond precision: matches the id's timestamp and round-trips through timestamptz.
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    UUID id = ids.newId(now);

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      ShortLink link = new ShortLink(id, codes.next(), target.value(), LinkState.ACTIVE, now, now);
      try {
        StorageFailures.run(() -> store.insert(link));
        return ShortLinkView.of(link, properties);
      } catch (ShortCodeTaken taken) {
        // Try again with a fresh code.
      }
    }
    throw new LinkStorageUnavailable(new ShortCodeTaken());
  }
}
