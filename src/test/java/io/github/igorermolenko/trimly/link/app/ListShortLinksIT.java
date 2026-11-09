package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.NOW;
import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ListShortLinksIT {

  @Autowired ListShortLinks listShortLinks;
  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void anEmptyListIsNotAnError() {
    ShortLinkPage page = listShortLinks.list(null, null);

    assertThat(page.items()).isEmpty();
    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void listsNewestFirstWithTheLaterOfSameMomentLinksFirst() {
    ShortLink first = store(aShortLink().createdAt(NOW).build());
    ShortLink second = store(aShortLink().createdAt(NOW).disabled().build());
    ShortLink third = store(aShortLink().createdAt(NOW.plusSeconds(1)).build());

    ShortLinkPage page = listShortLinks.list(null, null);

    assertThat(page.items())
        .extracting(ShortLinkView::shortCode)
        .containsExactly(third.shortCode(), second.shortCode(), first.shortCode());
    ShortLinkView disabled = page.items().get(1);
    assertThat(disabled.state()).isEqualTo(LinkState.DISABLED);
    assertThat(disabled.targetUrl()).isEqualTo(second.targetUrl());
    assertThat(disabled.createdAt()).isEqualTo(second.createdAt());
    assertThat(disabled.shortUrl()).endsWith("/" + second.shortCode().value());
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  void pagesTwentyByDefaultAndUpToAHundredOnRequest() {
    IntStream.range(0, 101).forEach(i -> store(aShortLink().build()));

    ShortLinkPage byDefault = listShortLinks.list(null, null);
    ShortLinkPage largest = listShortLinks.list(null, 100);

    assertThat(byDefault.items()).hasSize(20);
    assertThat(byDefault.hasNext()).isTrue();
    assertThat(largest.items()).hasSize(100);
    assertThat(largest.hasNext()).isTrue();
    assertThat(listShortLinks.list(largest.nextCursor(), 100).items()).hasSize(1);
  }

  @Test
  void exactlyOnePageOfLinksHasNoNextPage() {
    IntStream.range(0, 3).forEach(i -> store(aShortLink().build()));

    ShortLinkPage page = listShortLinks.list(null, 3);

    assertThat(page.items()).hasSize(3);
    assertThat(page.hasNext()).isFalse();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void linksCreatedWhilePagingAreNeitherDuplicatedNorSkipped() {
    List<ShortLink> existing = new ArrayList<>();
    IntStream.range(0, 5).forEach(i -> existing.add(store(aShortLink().build())));

    List<ShortCode> seen = new ArrayList<>();
    ShortLinkPage page = listShortLinks.list(null, 2);
    seen.addAll(page.items().stream().map(ShortLinkView::shortCode).toList());
    while (page.hasNext()) {
      store(aShortLink().build());
      page = listShortLinks.list(page.nextCursor(), 2);
      seen.addAll(page.items().stream().map(ShortLinkView::shortCode).toList());
    }

    assertThat(seen)
        .containsExactlyElementsOf(existing.reversed().stream().map(ShortLink::shortCode).toList());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 101})
  void rejectsAPageSizeOutsideOneToAHundred(int limit) {
    assertThatThrownBy(() -> listShortLinks.list(null, limit)).isInstanceOf(InvalidPageSize.class);
  }

  @Test
  void rejectsAGarbledCursor() {
    assertThatThrownBy(() -> listShortLinks.list("xyz", null)).isInstanceOf(InvalidCursor.class);
  }

  private ShortLink store(ShortLink link) {
    store.insert(link);
    return link;
  }
}
