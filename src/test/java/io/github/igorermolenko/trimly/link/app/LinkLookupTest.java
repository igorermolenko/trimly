package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.QueryTimeoutException;

class LinkLookupTest {

  private final ShortLinkStore store = mock(ShortLinkStore.class);
  private final LinkLookup lookup = new LinkLookup(store);

  @Test
  void anActiveLinkIsFoundWithItsExactTargetUrl() {
    ShortLink link = aShortLink().withTargetUrl("https://example.test/A%20b?c=D").build();
    when(store.findByShortCode(link.shortCode())).thenReturn(Optional.of(link));

    assertThat(lookup.resolve(link.shortCode().value()))
        .isEqualTo(new Resolution.Found("https://example.test/A%20b?c=D"));
  }

  @Test
  void aDisabledLinkAndAnUnknownCodeAreTheSameNotFound() {
    ShortLink disabled = aShortLink().disabled().build();
    when(store.findByShortCode(disabled.shortCode())).thenReturn(Optional.of(disabled));
    when(store.findByShortCode(ShortCode.of("zzzzzzz"))).thenReturn(Optional.empty());

    assertThat(lookup.resolve(disabled.shortCode().value()))
        .isEqualTo(Resolution.NotFound.INSTANCE);
    assertThat(lookup.resolve("zzzzzzz")).isEqualTo(Resolution.NotFound.INSTANCE);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"abc2345.", "abc234", "abc23456", "abc0345", "abc2345/x"})
  void aMalformedSegmentIsNotFoundWithoutAskingStorage(String segment) {
    when(store.findByShortCode(any())).thenThrow(new QueryTimeoutException("down"));

    assertThat(lookup.resolve(segment)).isEqualTo(Resolution.NotFound.INSTANCE);
    verifyNoInteractions(store);
  }

  @Test
  void aStorageTimeoutIsUnavailableNeverNotFound() {
    when(store.findByShortCode(any())).thenThrow(new QueryTimeoutException("down"));

    assertThat(lookup.resolve("abc2345")).isEqualTo(Resolution.Unavailable.INSTANCE);
  }

  @Test
  void anUnexpectedFailurePropagates() {
    when(store.findByShortCode(any())).thenThrow(new IllegalStateException("bug"));

    assertThatThrownBy(() -> lookup.resolve("abc2345")).isInstanceOf(IllegalStateException.class);
  }
}
