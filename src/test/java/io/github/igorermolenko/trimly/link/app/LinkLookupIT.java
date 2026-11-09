package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LinkLookupIT {

  @Autowired LinkLookup lookup;
  @Autowired ShortLinkStore store;
  @Autowired DisableShortLink disableShortLink;
  @Autowired EnableShortLink enableShortLink;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void resolvesAnActiveLinkToItsExactTargetUrl() {
    ShortLink link = aShortLink().withTargetUrl("https://sub.example.test/P%C3%A4th?q=1#x").build();
    store.insert(link);

    assertThat(lookup.resolve(link.shortCode().value()))
        .isEqualTo(new Resolution.Found("https://sub.example.test/P%C3%A4th?q=1#x"));
  }

  @Test
  void disabledAndUnknownAreNotFound() {
    ShortLink disabled = aShortLink().disabled().build();
    store.insert(disabled);

    assertThat(lookup.resolve(disabled.shortCode().value()))
        .isEqualTo(Resolution.NotFound.INSTANCE);
    assertThat(lookup.resolve("zzzzzzz")).isEqualTo(Resolution.NotFound.INSTANCE);
  }

  @Test
  void readsStorageEveryTimeSoAStateChangeShowsAtOnce() {
    ShortLink link = aShortLink().build();
    store.insert(link);
    String code = link.shortCode().value();

    assertThat(lookup.resolve(code)).isInstanceOf(Resolution.Found.class);
    disableShortLink.disable(code);
    assertThat(lookup.resolve(code)).isEqualTo(Resolution.NotFound.INSTANCE);
    enableShortLink.enable(code);
    assertThat(lookup.resolve(code)).isEqualTo(new Resolution.Found(link.targetUrl()));
  }
}
