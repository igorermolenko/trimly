package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import org.springframework.stereotype.Service;

/** Disables a short link; repeating it is harmless (AC-14). */
@Service
public class DisableShortLink {

  private final ChangeLinkState changeLinkState;

  DisableShortLink(ChangeLinkState changeLinkState) {
    this.changeLinkState = changeLinkState;
  }

  /**
   * @throws LinkNotFound when no link has {@code shortCode}, or it cannot be a short code
   * @throws LinkStorageUnavailable when storage does not answer in time
   */
  public LinkStateView disable(String shortCode) {
    return changeLinkState.change(shortCode, LinkState.DISABLED);
  }
}
