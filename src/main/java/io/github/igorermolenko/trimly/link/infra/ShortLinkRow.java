package io.github.igorermolenko.trimly.link.infra;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("short_link")
record ShortLinkRow(
    @Id UUID id,
    String shortCode,
    String targetUrl,
    String state,
    Instant createdAt,
    Instant updatedAt) {}
