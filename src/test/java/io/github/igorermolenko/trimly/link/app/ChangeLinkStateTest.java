package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

class ChangeLinkStateTest {

  private final ShortLinkStore store = mock(ShortLinkStore.class);
  private final DisableShortLink disable = new DisableShortLink(new ChangeLinkState(store, CLOCK));
  private final EnableShortLink enable = new EnableShortLink(new ChangeLinkState(store, CLOCK));

  @ParameterizedTest
  @ValueSource(strings = {"abc2345.", "abc234", "abc0345", "abc2345/x", ""})
  void aMalformedCodeIsNotFoundWithoutAskingStorage(String code) {
    assertThatThrownBy(() -> disable.disable(code)).isInstanceOf(LinkNotFound.class);
    assertThatThrownBy(() -> enable.enable(code)).isInstanceOf(LinkNotFound.class);
    verifyNoInteractions(store);
  }

  @Test
  void storageDownIsUnavailable() {
    when(store.setState(any(), any(), any()))
        .thenThrow(new CannotGetJdbcConnectionException("timeout"));

    assertThatThrownBy(() -> disable.disable("abc2345")).isInstanceOf(LinkStorageUnavailable.class);
  }
}
