package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import org.springframework.stereotype.Service;

/** Re-enables a short link under the same code and target URL; repeating it is harmless (AC-14). */
@Service
public class EnableShortLink {

  private final ChangeLinkState changeLinkState;

  EnableShortLink(ChangeLinkState changeLinkState) {
    this.changeLinkState = changeLinkState;
  }

  /**
   * @throws LinkNotFound when no link has {@code shortCode}, or it cannot be a short code
   * @throws LinkStorageUnavailable when storage does not answer in time
   */
  public LinkStateView enable(String shortCode) {
    return changeLinkState.change(shortCode, LinkState.ACTIVE);
  }
}
