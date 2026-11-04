package io.github.igorermolenko.trimly.link.infra;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.NOW;
import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortCodeTaken;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShortLinkStoreIT {

  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void insertsAndFindsALinkByItsCode() {
    ShortLink link = aShortLink().withTargetUrl("https://example.test/a?b=C%20d#e").build();

    store.insert(link);

    assertThat(store.findByShortCode(link.shortCode())).contains(link);
  }

  @Test
  void findingAnUnknownCodeIsEmpty() {
    assertThat(store.findByShortCode(ShortCode.of("zzzzzzz"))).isEmpty();
  }

  @Test
  void findingIsCaseSensitive() {
    ShortLink link = aShortLink().withShortCode("abcdefg").build();
    store.insert(link);

    assertThat(store.findByShortCode(ShortCode.of("ABCDEFG"))).isEmpty();
  }

  @Test
  void aTakenCodeIsReportedAsShortCodeTaken() {
    store.insert(aShortLink().withShortCode("abc2345").build());

    assertThatThrownBy(() -> store.insert(aShortLink().withShortCode("abc2345").build()))
        .isInstanceOf(ShortCodeTaken.class);
  }

  @Test
  void anotherConstraintViolationPropagatesUnchanged() {
    ShortLink tooLong =
        aShortLink().withTargetUrl("https://example.test/" + "a".repeat(3000)).build();

    assertThatThrownBy(() -> store.insert(tooLong))
        .isInstanceOf(DataIntegrityViolationException.class)
        .isNotInstanceOf(ShortCodeTaken.class);
  }

  @Test
  void pagesNewestFirstWithOneRowOfLookAhead() {
    ShortLink first = aShortLink().createdAt(NOW).build();
    ShortLink second = aShortLink().createdAt(NOW).build();
    ShortLink third = aShortLink().createdAt(NOW.plusSeconds(1)).build();
    List.of(first, second, third).forEach(store::insert);

    assertThat(store.page(null, 2)).containsExactly(third, second, first);
    assertThat(store.page(null, 5)).containsExactly(third, second, first);
    assertThat(store.page(third.id(), 1)).containsExactly(second, first);
    assertThat(store.page(first.id(), 5)).isEmpty();
  }

  @Test
  void pagingAnEmptyTableIsEmpty() {
    assertThat(store.page(null, 20)).isEmpty();
  }

  @Test
  void changingTheStateStampsUpdatedAt() {
    ShortLink link = aShortLink().build();
    store.insert(link);
    Instant later = NOW.plusSeconds(60);

    assertThat(store.setState(link.shortCode(), LinkState.DISABLED, later))
        .hasValueSatisfying(changed -> {
          assertThat(changed.state()).isEqualTo(LinkState.DISABLED);
          assertThat(changed.updatedAt()).isEqualTo(later);
          assertThat(changed.createdAt()).isEqualTo(link.createdAt());
          assertThat(changed.targetUrl()).isEqualTo(link.targetUrl());
        });
    assertThat(store.findByShortCode(link.shortCode()))
        .hasValueSatisfying(found -> assertThat(found.state()).isEqualTo(LinkState.DISABLED));
  }

  @Test
  void settingTheCurrentStateAgainLeavesUpdatedAtAlone() {
    ShortLink link = aShortLink().build();
    store.insert(link);

    assertThat(store.setState(link.shortCode(), LinkState.ACTIVE, NOW.plusSeconds(60)))
        .hasValueSatisfying(same -> {
          assertThat(same.state()).isEqualTo(LinkState.ACTIVE);
          assertThat(same.updatedAt()).isEqualTo(link.updatedAt());
        });
  }

  @Test
  void settingTheStateOfAnUnknownCodeIsEmpty() {
    assertThat(store.setState(ShortCode.of("zzzzzzz"), LinkState.DISABLED, NOW)).isEmpty();
  }
}
