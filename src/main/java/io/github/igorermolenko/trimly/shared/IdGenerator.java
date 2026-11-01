package io.github.igorermolenko.trimly.shared;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates primary keys as UUIDv7 (RFC 9562): a 48-bit Unix millisecond timestamp followed by
 * random bits, so keys sort roughly by creation time.
 */
@Component
public class IdGenerator {

  private final SecureRandom random = new SecureRandom();
  private final Clock clock;

  public IdGenerator(Clock clock) {
    this.clock = clock;
  }

  public UUID newId() {
    long millis = clock.millis();
    long randA = random.nextInt(1 << 12);
    long randB = random.nextLong();

    long msb = (millis & 0xFFFF_FFFF_FFFFL) << 16 | 0x7000L | randA;
    long lsb = (randB & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
    return new UUID(msb, lsb);
  }
}
