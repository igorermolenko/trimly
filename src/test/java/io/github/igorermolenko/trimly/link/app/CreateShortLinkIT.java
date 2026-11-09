package io.github.igorermolenko.trimly.link.app;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CreateShortLinkIT {

  @Autowired CreateShortLink createShortLink;
  @Autowired ShortLinkStore store;

  @Test
  void storesAnActiveLinkWithTheTargetUrlByteForByte() {
    String target = "https://example.test/Path/%7Euser?q=A+b&r=%C3%A9#Frag";

    ShortLinkView created = createShortLink.create(target);

    assertThat(store.findByShortCode(created.shortCode()))
        .hasValueSatisfying(stored -> {
          assertThat(stored.targetUrl()).isEqualTo(target);
          assertThat(stored.state()).isEqualTo(LinkState.ACTIVE);
          assertThat(stored.createdAt()).isEqualTo(stored.updatedAt());
          assertThat(stored.createdAt()).isEqualTo(created.createdAt());
        });
  }
}
