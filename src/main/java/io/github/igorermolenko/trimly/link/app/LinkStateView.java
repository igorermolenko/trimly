package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;

/** A link's state after disable or re-enable. */
public record LinkStateView(ShortCode shortCode, LinkState state) {}
