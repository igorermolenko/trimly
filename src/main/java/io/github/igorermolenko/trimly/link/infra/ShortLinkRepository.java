package io.github.igorermolenko.trimly.link.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/** The short_link access patterns from data-model.md, one SQL statement each. */
interface ShortLinkRepository extends Repository<ShortLinkRow, UUID> {

  @Modifying
  @Query(
      """
      INSERT INTO short_link (id, short_code, target_url, state, created_at, updated_at)
      VALUES (:id, :shortCode, :targetUrl, :state, :createdAt, :updatedAt)
      """)
  void insert(
      UUID id,
      String shortCode,
      String targetUrl,
      String state,
      Instant createdAt,
      Instant updatedAt);

  Optional<ShortLinkRow> findByShortCode(String shortCode);

  @Query("SELECT * FROM short_link ORDER BY id DESC LIMIT :limit")
  List<ShortLinkRow> newest(int limit);

  @Query("SELECT * FROM short_link WHERE id < :after ORDER BY id DESC LIMIT :limit")
  List<ShortLinkRow> newestBefore(UUID after, int limit);

  @Query(
      """
      UPDATE short_link
         SET updated_at = CASE WHEN state <> :state THEN :now ELSE updated_at END,
             state = :state
       WHERE short_code = :shortCode
      RETURNING *
      """)
  Optional<ShortLinkRow> setState(String shortCode, String state, Instant now);
}
