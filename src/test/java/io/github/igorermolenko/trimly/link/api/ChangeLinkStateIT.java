package io.github.igorermolenko.trimly.link.api;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static io.github.igorermolenko.trimly.link.api.OwnerApiTestSupport.ownerPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import io.github.igorermolenko.trimly.link.app.DisableShortLink;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ChangeLinkStateIT {

  @Autowired MockMvc mvc;
  @Autowired OwnerCredentialProperties credential;
  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean DisableShortLink disableShortLink;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void disablesAnActiveLinkAndDisablingAgainConfirmsIt() throws Exception {
    ShortLink link = stored(aShortLink().build());

    for (int i = 0; i < 2; i++) {
      mvc.perform(ownerPost(token(), "/api/v1/links/{code}/disable", link.shortCode().value()))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      "{\"shortCode\":\"" + link.shortCode().value() + "\",\"state\":\"DISABLED\"}",
                      org.springframework.test.json.JsonCompareMode.STRICT));
    }
    assertThat(store.findByShortCode(link.shortCode()).orElseThrow().state())
        .isEqualTo(LinkState.DISABLED);
  }

  @Test
  void reEnablesADisabledLink() throws Exception {
    ShortLink link = stored(aShortLink().disabled().build());

    for (int i = 0; i < 2; i++) {
      mvc.perform(ownerPost(token(), "/api/v1/links/{code}/enable", link.shortCode().value()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.shortCode").value(link.shortCode().value()))
          .andExpect(jsonPath("$.state").value("ACTIVE"));
    }
    ShortLink after = store.findByShortCode(link.shortCode()).orElseThrow();
    assertThat(after.state()).isEqualTo(LinkState.ACTIVE);
    assertThat(after.targetUrl()).isEqualTo(link.targetUrl());
  }

  @ParameterizedTest
  @ValueSource(strings = {"zzzzzzz", "abc2345.", "abc0345", "toolongcode"})
  void anUnknownOrMalformedCodeIsNotFound(String code) throws Exception {
    for (String action : new String[] {"disable", "enable"}) {
      mvc.perform(ownerPost(token(), "/api/v1/links/{code}/" + action, code))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("link.not_found"))
          .andExpect(jsonPath("$.detail").value("The link was not found."));
    }
  }

  @Test
  void storageDownIsTemporarilyUnavailable() throws Exception {
    doThrow(new LinkStorageUnavailable(new RuntimeException()))
        .when(disableShortLink)
        .disable(any());

    mvc.perform(ownerPost(token(), "/api/v1/links/{code}/disable", "abc2345"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("link.temporarily_unavailable"));
  }

  private ShortLink stored(ShortLink link) {
    store.insert(link);
    return link;
  }

  private String token() {
    return credential.ownerCredential();
  }
}
