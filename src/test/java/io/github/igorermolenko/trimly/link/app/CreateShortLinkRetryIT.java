package io.github.igorermolenko.trimly.link.app;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import io.github.igorermolenko.trimly.link.domain.ShortCodeGenerator;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * AC-20b against real storage: a generated code that is already taken hits the real unique
 * constraint and is retried with a fresh one, up to 5 attempts (ADR-0002).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CreateShortLinkRetryIT {

  private static final String FREE = "free234";

  @Autowired CreateShortLink createShortLink;
  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean ShortCodeGenerator codes;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void oneTakenCodeIsRetriedWithAFreshOne() {
    String[] taken = taken(1);
    generates(taken, FREE);

    assertThat(createShortLink.create("https://example.test/").shortCode().value()).isEqualTo(FREE);
    assertThat(storedLinks()).isEqualTo(2);
  }

  @Test
  void fourTakenCodesThenAFreeOneStillCreatesTheLink() {
    String[] taken = taken(4);
    generates(taken, FREE);

    assertThat(createShortLink.create("https://example.test/").shortCode().value()).isEqualTo(FREE);
    assertThat(storedLinks()).isEqualTo(5);
  }

  @Test
  void fiveTakenCodesAreTemporarilyUnavailableAndCreateNothing() {
    String[] taken = taken(5);
    generates(taken, FREE);

    assertThatThrownBy(() -> createShortLink.create("https://example.test/"))
        .isInstanceOf(LinkStorageUnavailable.class);
    assertThat(storedLinks()).isEqualTo(5);
    assertThat(store.findByShortCode(ShortCode.of(FREE))).isEmpty();
  }

  /** {@code n} codes, each already held by a stored link. */
  private String[] taken(int n) {
    String[] codes =
        Stream.of("tkna234", "tknb234", "tknc234", "tknd234", "tkne234")
            .limit(n)
            .toArray(String[]::new);
    Arrays.stream(codes).forEach(code -> store.insert(aShortLink().withShortCode(code).build()));
    return codes;
  }

  private void generates(String[] first, String then) {
    ShortCode[] rest =
        Stream.concat(Arrays.stream(first).skip(1), Stream.of(then))
            .map(ShortCode::of)
            .toArray(ShortCode[]::new);
    when(codes.next()).thenReturn(ShortCode.of(first[0]), rest);
  }

  private int storedLinks() {
    return jdbc.queryForObject("SELECT count(*) FROM short_link", Integer.class);
  }
}
