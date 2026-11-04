package io.github.igorermolenko.trimly.link.infra;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortCodeTaken;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * Short-link persistence. Every method is one SQL statement with no surrounding transaction, so a
 * failed insert never poisons a retry (ADR-0002).
 */
@Component
public class ShortLinkStore {

  static final String UNIQUE_SHORT_CODE = "uq_short_link_short_code";

  private final ShortLinkRepository repository;

  ShortLinkStore(ShortLinkRepository repository) {
    this.repository = repository;
  }

  /**
   * Inserts a new link.
   *
   * @throws ShortCodeTaken when another link already holds its short code
   */
  public void insert(ShortLink link) {
    try {
      repository.insert(
          link.id(),
          link.shortCode().value(),
          link.targetUrl(),
          link.state().name(),
          link.createdAt(),
          link.updatedAt());
    } catch (DuplicateKeyException e) {
      if (namesUniqueShortCode(e)) {
        throw new ShortCodeTaken();
      }
      throw e;
    }
  }

  public Optional<ShortLink> findByShortCode(ShortCode code) {
    return repository.findByShortCode(code.value()).map(ShortLinkStore::toDomain);
  }

  /**
   * Up to {@code size + 1} links, newest first, after the link with id {@code after} (from the
   * newest when null). The extra row tells the caller whether more follow.
   */
  public List<ShortLink> page(UUID after, int size) {
    List<ShortLinkRow> rows =
        after == null ? repository.newest(size + 1) : repository.newestBefore(after, size + 1);
    return rows.stream().map(ShortLinkStore::toDomain).toList();
  }

  /**
   * Sets the state of the link with {@code code}, stamping {@code now} only on a real change.
   *
   * @return the link as it now is, or empty when no link has that code
   */
  public Optional<ShortLink> setState(ShortCode code, LinkState state, Instant now) {
    return repository.setState(code.value(), state.name(), now).map(ShortLinkStore::toDomain);
  }

  private static boolean namesUniqueShortCode(DuplicateKeyException e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t.getMessage() != null && t.getMessage().contains(UNIQUE_SHORT_CODE)) {
        return true;
      }
    }
    return false;
  }

  private static ShortLink toDomain(ShortLinkRow row) {
    return new ShortLink(
        row.id(),
        ShortCode.of(row.shortCode()),
        row.targetUrl(),
        LinkState.valueOf(row.state()),
        row.createdAt(),
        row.updatedAt());
  }
}
