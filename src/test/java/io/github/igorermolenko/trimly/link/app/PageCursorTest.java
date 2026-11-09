package io.github.igorermolenko.trimly.link.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.link.ShortLinkFixtures;
import io.github.igorermolenko.trimly.shared.IdGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PageCursorTest {

  @Test
  void roundTripsTheLastLinksId() {
    UUID id = new IdGenerator(ShortLinkFixtures.CLOCK).newId();

    String token = PageCursor.encode(id);

    assertThat(token).isEqualTo(id.toString());
    assertThat(PageCursor.decode(token)).isEqualTo(id);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "xyz",
        "3f1c2a9e-5b7d-4c11-8a2e-9d0f1b2c3d4e", // version 4
        "019a4c3e-1d2f-7abc-0def-123456789abc", // version 7, wrong variant
        "019A4C3E-1D2F-7ABC-8DEF-123456789ABC", // not the canonical lower-case form
        "19a4c3e-1d2f-7abc-8def-123456789abc",
        "019a4c3e-1d2f-7abc-8def-123456789abc-extra",
      })
  void rejectsAnythingThisApiDidNotIssue(String token) {
    assertThatThrownBy(() -> PageCursor.decode(token)).isInstanceOf(InvalidCursor.class);
  }
}
