package io.github.igorermolenko.trimly.link;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.shared.IdGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;

/**
 * Test data builder for {@link ShortLink} (data-model §Test fixtures). Codes come from the real
 * generator with a seeded {@link Random}; target URLs use only {@code example.test} hosts.
 */
public final class ShortLinkFixtures {

  public static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
  public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  private static final ShortCodeGenerator CODES = ShortCodeGenerator.from(new Random(20261104));
  private static final IdGenerator IDS = new IdGenerator(CLOCK);

  private ShortLinkFixtures() {}

  public static Builder aShortLink() {
    return new Builder();
  }

  /** Builds one link; each new builder gets a fresh id and code. */
  public static final class Builder {
    private UUID id;
    private ShortCode shortCode;
    private String targetUrl = "https://example.test/articles/1";
    private LinkState state = LinkState.ACTIVE;
    private Instant createdAt = NOW;
    private Instant updatedAt;

    private Builder() {
      synchronized (ShortLinkFixtures.class) {
        id = IDS.newId();
        shortCode = CODES.next();
      }
    }

    public Builder disabled() {
      state = LinkState.DISABLED;
      return this;
    }

    public Builder withShortCode(String code) {
      shortCode = ShortCode.of(code);
      return this;
    }

    public Builder withTargetUrl(String url) {
      targetUrl = url;
      return this;
    }

    public Builder createdAt(Instant at) {
      createdAt = at;
      synchronized (ShortLinkFixtures.class) {
        id = IDS.newId(at);
      }
      return this;
    }

    public Builder withId(UUID newId) {
      id = newId;
      return this;
    }

    public ShortLink build() {
      return new ShortLink(
          id, shortCode, targetUrl, state, createdAt, updatedAt == null ? createdAt : updatedAt);
    }
  }
}
