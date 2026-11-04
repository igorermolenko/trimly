package io.github.igorermolenko.trimly.redirect.app;

import io.github.igorermolenko.trimly.link.app.LinkLookup;
import io.github.igorermolenko.trimly.link.app.Resolution;
import org.springframework.stereotype.Service;

/**
 * Turns a link lookup into the visitor's answer. The exhaustive switch handles every {@link
 * Resolution}, so no outcome can fall through to a JSON error (ADR-0005). Nothing about visitors is
 * logged.
 */
@Service
public class FollowShortLink {

  private final LinkLookup lookup;

  FollowShortLink(LinkLookup lookup) {
    this.lookup = lookup;
  }

  public VisitorAnswer follow(String segment) {
    return switch (lookup.resolve(segment)) {
      case Resolution.Found found -> new VisitorAnswer.Redirect(found.targetUrl());
      case Resolution.NotFound notFound -> VisitorAnswer.Message.NOT_FOUND;
      case Resolution.Unavailable unavailable -> VisitorAnswer.Message.UNAVAILABLE;
    };
  }
}
