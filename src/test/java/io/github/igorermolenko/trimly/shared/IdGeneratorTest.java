package io.github.igorermolenko.trimly.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

  private static final Instant NOW = Instant.parse("2026-11-01T12:00:00.123Z");

  private final IdGenerator generator = new IdGenerator(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void generatesVersion7WithRfcVariant() {
    UUID id = generator.newId();

    assertThat(id.version()).isEqualTo(7);
    assertThat(id.variant()).isEqualTo(2);
  }

  @Test
  void embedsTheClockMillisInTheTopBits() {
    UUID id = generator.newId();

    assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
  }

  @Test
  void idsFromLaterMillisSortAfterEarlierOnes() {
    UUID earlier = generator.newId();
    UUID later = new IdGenerator(Clock.fixed(NOW.plusMillis(1), ZoneOffset.UTC)).newId();

    assertThat(later.toString()).isGreaterThan(earlier.toString());
  }

  @Test
  void generatesDistinctIdsWithinTheSameMilli() {
    assertThat(generator.newId()).isNotEqualTo(generator.newId());
  }
}
