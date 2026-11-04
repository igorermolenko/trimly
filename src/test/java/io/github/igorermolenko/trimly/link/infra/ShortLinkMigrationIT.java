package io.github.igorermolenko.trimly.link.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** The promoted V2 migration creates short_link with its named constraints (AC-20, ADR-0002). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShortLinkMigrationIT {

  private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-11-04T10:00:00Z"));

  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void createsTheTableWithEveryNamedConstraint() {
    assertThat(
            jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = 'short_link'::regclass",
                String.class))
        .containsExactlyInAnyOrder(
            "pk_short_link",
            "uq_short_link_short_code",
            "ck_short_link_short_code_length",
            "ck_short_link_target_url_length",
            "ck_short_link_state");
  }

  @Test
  void rejectsADuplicateShortCodeByTheUniqueConstraintName() {
    insert("abc2345", "https://example.com/a", "ACTIVE");

    assertThatThrownBy(() -> insert("abc2345", "https://example.com/b", "ACTIVE"))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessageContaining("uq_short_link_short_code");
  }

  @Test
  void acceptsCodesThatDifferOnlyInLetterCase() {
    insert("abc2345", "https://example.com/a", "ACTIVE");
    insert("ABC2345", "https://example.com/b", "ACTIVE");

    assertThat(jdbc.queryForObject("SELECT count(*) FROM short_link", Integer.class)).isEqualTo(2);
  }

  @Test
  void rejectsAnUnknownStateAnEmptyTargetAndAWrongCodeLength() {
    assertThatThrownBy(() -> insert("abc2345", "https://example.com/a", "DELETED"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_short_link_state");
    assertThatThrownBy(() -> insert("abc2345", "", "ACTIVE"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_short_link_target_url_length");
    assertThatThrownBy(() -> insert("abc234", "https://example.com/a", "ACTIVE"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ck_short_link_short_code_length");
  }

  private void insert(String code, String target, String state) {
    jdbc.update(
        "INSERT INTO short_link (id, short_code, target_url, state, created_at, updated_at)"
            + " VALUES (?, ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        code,
        target,
        state,
        NOW,
        NOW);
  }
}
