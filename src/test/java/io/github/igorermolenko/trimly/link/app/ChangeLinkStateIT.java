package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkNotFound;
import io.github.igorermolenko.trimly.link.domain.LinkState;
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
class ChangeLinkStateIT {

  @Autowired DisableShortLink disableShortLink;
  @Autowired EnableShortLink enableShortLink;
  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void disablingAnActiveLinkRecordsAndConfirmsIt() {
    ShortLink link = stored(aShortLink().build());

    LinkStateView result = disableShortLink.disable(link.shortCode().value());

    assertThat(result).isEqualTo(new LinkStateView(link.shortCode(), LinkState.DISABLED));
    ShortLink after = store.findByShortCode(link.shortCode()).orElseThrow();
    assertThat(after.state()).isEqualTo(LinkState.DISABLED);
    assertThat(after.updatedAt()).isNotEqualTo(link.updatedAt());
  }

  @Test
  void disablingAgainConfirmsWithoutChangingAnything() {
    ShortLink link = stored(aShortLink().disabled().build());

    LinkStateView result = disableShortLink.disable(link.shortCode().value());

    assertThat(result.state()).isEqualTo(LinkState.DISABLED);
    assertThat(store.findByShortCode(link.shortCode()).orElseThrow()).isEqualTo(link);
  }

  @Test
  void enablingAgainConfirmsWithoutChangingAnything() {
    ShortLink link = stored(aShortLink().build());

    assertThat(enableShortLink.enable(link.shortCode().value()).state())
        .isEqualTo(LinkState.ACTIVE);
    assertThat(store.findByShortCode(link.shortCode()).orElseThrow()).isEqualTo(link);
  }

  @Test
  void reEnablingKeepsTheSameCodeAndTargetUrl() {
    ShortLink link = stored(aShortLink().disabled().build());

    LinkStateView result = enableShortLink.enable(link.shortCode().value());

    assertThat(result).isEqualTo(new LinkStateView(link.shortCode(), LinkState.ACTIVE));
    ShortLink after = store.findByShortCode(link.shortCode()).orElseThrow();
    assertThat(after.state()).isEqualTo(LinkState.ACTIVE);
    assertThat(after.targetUrl()).isEqualTo(link.targetUrl());
    assertThat(after.id()).isEqualTo(link.id());
  }

  @Test
  void anUnknownCodeIsNotFoundAndChangesNothing() {
    ShortLink other = stored(aShortLink().build());

    assertThatThrownBy(() -> disableShortLink.disable("zzzzzzz")).isInstanceOf(LinkNotFound.class);
    assertThatThrownBy(() -> enableShortLink.enable("zzzzzzz")).isInstanceOf(LinkNotFound.class);
    assertThat(store.findByShortCode(other.shortCode()).orElseThrow()).isEqualTo(other);
  }

  private ShortLink stored(ShortLink link) {
    store.insert(link);
    return link;
  }
}
