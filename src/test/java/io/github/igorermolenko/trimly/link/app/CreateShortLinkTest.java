package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.CLOCK;
import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import io.github.igorermolenko.trimly.link.domain.ShortCodeTaken;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import io.github.igorermolenko.trimly.shared.IdGenerator;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;

class CreateShortLinkTest {

  private static final String TARGET = "https://example.test/articles/1?x=A%20b";

  private final ShortLinkStore store = mock(ShortLinkStore.class);
  private final Deque<String> codes = new ArrayDeque<>();
  private final ShortCodeGenerator generator = () -> ShortCode.of(codes.removeFirst());
  private final LinkProperties properties =
      new LinkProperties(URI.create("https://trimly.example.test"), Set.of("trimly.example.test"));
  private final CreateShortLink createShortLink =
      new CreateShortLink(store, generator, new IdGenerator(CLOCK), CLOCK, properties);

  @Test
  void createsAnActiveLinkWithTheFullShortUrlFromThePublicBaseUrl() {
    codes.add("abc2345");

    ShortLinkView created = createShortLink.create(TARGET);

    assertThat(created.shortCode()).isEqualTo(ShortCode.of("abc2345"));
    assertThat(created.shortUrl()).isEqualTo("https://trimly.example.test/abc2345");
    assertThat(created.targetUrl()).isEqualTo(TARGET);
    assertThat(created.state()).isEqualTo(LinkState.ACTIVE);
    assertThat(created.createdAt()).isEqualTo(NOW);

    ShortLink stored = insertedLinks(1).getFirst();
    assertThat(stored.targetUrl()).isEqualTo(TARGET);
    assertThat(stored.state()).isEqualTo(LinkState.ACTIVE);
    assertThat(stored.createdAt()).isEqualTo(NOW);
    assertThat(stored.updatedAt()).isEqualTo(NOW);
    assertThat(stored.id().getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
  }

  @Test
  void aRejectedTargetUrlStoresNothing() {
    assertThatThrownBy(() -> createShortLink.create("ftp://example.test/"))
        .isInstanceOf(TargetUrlRejected.class);
    assertThatThrownBy(() -> createShortLink.create("https://trimly.example.test/abc2345"))
        .isInstanceOf(TargetUrlRejected.class);

    verifyNoInteractions(store);
  }

  @Test
  void theSameTargetTwiceGivesTwoSeparateLinks() {
    codes.addAll(List.of("abc2345", "xyz6789"));

    ShortLinkView first = createShortLink.create(TARGET);
    ShortLinkView second = createShortLink.create(TARGET);

    assertThat(second.shortCode()).isNotEqualTo(first.shortCode());
    List<ShortLink> inserted = insertedLinks(2);
    assertThat(inserted.get(0).id()).isNotEqualTo(inserted.get(1).id());
  }

  @Test
  void retriesWithAFreshCodeWhileCodesAreTaken() {
    codes.addAll(List.of("aaaaaaa", "bbbbbbb", "ccccccc", "ddddddd", "eeeeeee"));
    doThrow(new ShortCodeTaken())
        .doThrow(new ShortCodeTaken())
        .doThrow(new ShortCodeTaken())
        .doThrow(new ShortCodeTaken())
        .doNothing()
        .when(store)
        .insert(any());

    ShortLinkView created = createShortLink.create(TARGET);

    assertThat(created.shortCode()).isEqualTo(ShortCode.of("eeeeeee"));
    assertThat(insertedLinks(5))
        .extracting(l -> l.shortCode().value())
        .containsExactly("aaaaaaa", "bbbbbbb", "ccccccc", "ddddddd", "eeeeeee");
  }

  @Test
  void fiveTakenCodesMeanTemporarilyUnavailable() {
    codes.addAll(List.of("aaaaaaa", "bbbbbbb", "ccccccc", "ddddddd", "eeeeeee", "fffffff"));
    doThrow(new ShortCodeTaken()).when(store).insert(any());

    assertThatThrownBy(() -> createShortLink.create(TARGET))
        .isInstanceOf(LinkStorageUnavailable.class);
    verify(store, times(5)).insert(any());
  }

  @Test
  void aStorageTimeoutIsUnavailableAtOnceAndNotRetriedAsTaken() {
    codes.addAll(List.of("aaaaaaa", "bbbbbbb"));
    doThrow(new QueryTimeoutException("timeout")).when(store).insert(any());

    assertThatThrownBy(() -> createShortLink.create(TARGET))
        .isInstanceOf(LinkStorageUnavailable.class);
    verify(store, times(1)).insert(any());
  }

  private List<ShortLink> insertedLinks(int count) {
    ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
    verify(store, times(count)).insert(captor.capture());
    return captor.getAllValues();
  }
}
