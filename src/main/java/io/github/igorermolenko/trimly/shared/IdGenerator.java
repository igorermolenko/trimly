package io.github.igorermolenko.trimly.shared;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates primary keys as UUIDv7 (RFC 9562): a 48-bit Unix millisecond timestamp followed by
 * random bits, so keys sort by creation time.
 *
 * <p>Ids are strictly increasing within this process (RFC 9562 §6.2, method 1): within one
 * millisecond a 12-bit counter in {@code rand_a} is incremented; when it overflows, the timestamp
 * advances by one millisecond; and when the clock steps backwards, the generator keeps the last
 * timestamp instead of going back.
 */
@Component
public class IdGenerator {

  private static final int COUNTER_BITS = 12;
  private static final int COUNTER_MAX = (1 << COUNTER_BITS) - 1;
  // A new millisecond seeds the counter in its lower half, leaving room for at least 2048
  // increments.
  private static final int COUNTER_SEED_BOUND = 1 << (COUNTER_BITS - 1);

  private final SecureRandom random = new SecureRandom();
  private final Clock clock;

  private long lastMillis = Long.MIN_VALUE;
  private int counter;

  public IdGenerator(Clock clock) {
    this.clock = clock;
  }

  public UUID newId() {
    return newId(clock.instant());
  }

  /**
   * A new id stamped with {@code at}, so a caller can record the same instant as its creation time.
   */
  public synchronized UUID newId(Instant at) {
    long millis = at.toEpochMilli();
    if (millis > lastMillis) {
      lastMillis = millis;
      counter = random.nextInt(COUNTER_SEED_BOUND);
    } else if (counter < COUNTER_MAX) {
      counter++;
    } else {
      lastMillis++;
      counter = random.nextInt(COUNTER_SEED_BOUND);
    }

    long msb = (lastMillis & 0xFFFF_FFFF_FFFFL) << 16 | 0x7000L | counter;
    long lsb = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
    return new UUID(msb, lsb);
  }
}
