package io.github.igorermolenko.trimly.link.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A short code bound for good to one target URL. Only {@code state} and {@code updatedAt} ever
 * change; rows are never deleted, so a code is never reissued (AC-20).
 *
 * @param targetUrl exactly as submitted, never normalised
 * @param updatedAt the time of the last real state change; equals {@code createdAt} until then
 */
public record ShortLink(
    UUID id,
    ShortCode shortCode,
    String targetUrl,
    LinkState state,
    Instant createdAt,
    Instant updatedAt) {}
