package io.github.igorermolenko.trimly.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

  @Test
  void idsWithinTheSameMilliAreStrictlyIncreasing() {
    List<String> ids = generate(generator, 1_000);

    assertThat(ids).isSortedAccordingTo(String::compareTo).doesNotHaveDuplicates();
    assertThat(ids)
        .allSatisfy(id ->
            assertThat(UUID.fromString(id).getMostSignificantBits() >>> 16)
                .isEqualTo(NOW.toEpochMilli()));
  }

  @Test
  void counterOverflowAdvancesTheTimestampAndKeepsTheOrder() {
    List<String> ids = generate(generator, 10_000);

    assertThat(ids).isSortedAccordingTo(String::compareTo).doesNotHaveDuplicates();
    UUID last = UUID.fromString(ids.getLast());
    assertThat(last.getMostSignificantBits() >>> 16).isGreaterThan(NOW.toEpochMilli());
    assertThat(last.version()).isEqualTo(7);
    assertThat(last.variant()).isEqualTo(2);
  }

  @Test
  void aClockSteppingBackwardsNeverProducesASmallerId() {
    MutableClock clock = new MutableClock(NOW);
    IdGenerator steppingGenerator = new IdGenerator(clock);

    String before = steppingGenerator.newId().toString();
    clock.instant = NOW.minusSeconds(5);
    String after = steppingGenerator.newId().toString();

    assertThat(after).isGreaterThan(before);
    assertThat(UUID.fromString(after).getMostSignificantBits() >>> 16)
        .isEqualTo(NOW.toEpochMilli());
  }

  @Test
  void stampsTheGivenInstant() {
    Instant at = NOW.plusSeconds(60);

    UUID id = generator.newId(at);

    assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(at.toEpochMilli());
  }

  private static List<String> generate(IdGenerator generator, int count) {
    List<String> ids = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ids.add(generator.newId().toString());
    }
    return ids;
  }

  private static final class MutableClock extends Clock {
    Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
