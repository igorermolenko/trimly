package io.github.igorermolenko.trimly.link.api;

import io.github.igorermolenko.trimly.link.app.DisableShortLink;
import io.github.igorermolenko.trimly.link.app.EnableShortLink;
import io.github.igorermolenko.trimly.link.app.LinkStateView;
import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disable and re-enable a short link; repeating either confirms the current state (AC-11…AC-15).
 */
@RestController
class ChangeLinkStateController {

  /** The {@code ShortLinkState} schema. */
  record ShortLinkStateResponse(String shortCode, LinkState state) {}

  private final DisableShortLink disableShortLink;
  private final EnableShortLink enableShortLink;

  ChangeLinkStateController(DisableShortLink disableShortLink, EnableShortLink enableShortLink) {
    this.disableShortLink = disableShortLink;
    this.enableShortLink = enableShortLink;
  }

  @PostMapping("/api/v1/links/{shortCode}/disable")
  ShortLinkStateResponse disable(@PathVariable String shortCode) {
    return answer(() -> disableShortLink.disable(shortCode));
  }

  @PostMapping("/api/v1/links/{shortCode}/enable")
  ShortLinkStateResponse enable(@PathVariable String shortCode) {
    return answer(() -> enableShortLink.enable(shortCode));
  }

  private static ShortLinkStateResponse answer(Supplier<LinkStateView> change) {
    try {
      LinkStateView view = change.get();
      return new ShortLinkStateResponse(view.shortCode().value(), view.state());
    } catch (LinkNotFound e) {
      throw LinkProblems.NOT_FOUND.exception();
    } catch (LinkStorageUnavailable e) {
      throw LinkProblems.TEMPORARILY_UNAVAILABLE.exception();
    }
  }
}
