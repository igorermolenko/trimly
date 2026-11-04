package io.github.igorermolenko.trimly.link.api;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static io.github.igorermolenko.trimly.link.api.OwnerApiTestSupport.ownerGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import io.github.igorermolenko.trimly.link.app.ListShortLinks;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "trimly.public-base-url=https://trimly.example.test")
@AutoConfigureMockMvc
class ListShortLinksIT {

  @Autowired MockMvc mvc;
  @Autowired OwnerCredentialProperties credential;
  @Autowired ShortLinkStore store;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean ListShortLinks listShortLinks;

  private final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void noLinksIsAnEmptyPage() throws Exception {
    mvc.perform(ownerGet(token(), "/api/v1/links"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()));
  }

  @Test
  void listsNewestFirstWithExactlyTheContractFields() throws Exception {
    ShortLink older = stored(aShortLink().build());
    ShortLink newer = stored(aShortLink().disabled().build());

    Map<String, Object> page = page("/api/v1/links");

    assertThat(page.keySet()).containsExactlyInAnyOrder("items", "hasNext", "nextCursor");
    List<Object> items = (List<Object>) page.get("items");
    Map<String, Object> first = (Map<String, Object>) items.getFirst();
    assertThat(first.keySet())
        .containsExactlyInAnyOrder("shortCode", "shortUrl", "targetUrl", "state", "createdAt");
    assertThat(first.get("shortCode")).isEqualTo(newer.shortCode().value());
    assertThat(first.get("state")).isEqualTo("DISABLED");
    assertThat(first.get("shortUrl"))
        .isEqualTo("https://trimly.example.test/" + newer.shortCode().value());
    assertThat(((Map<String, Object>) items.get(1)).get("shortCode"))
        .isEqualTo(older.shortCode().value());
  }

  @Test
  void pagesTwentyByDefaultAndContinuesAfterTheLastLink() throws Exception {
    List<ShortLink> links = new ArrayList<>();
    IntStream.range(0, 25).forEach(i -> links.add(stored(aShortLink().build())));

    Map<String, Object> first = page("/api/v1/links");
    assertThat((List<Object>) first.get("items")).hasSize(20);
    assertThat(first.get("hasNext")).isEqualTo(true);

    Map<String, Object> second = page("/api/v1/links?limit=100&after=" + first.get("nextCursor"));
    List<Object> rest = (List<Object>) second.get("items");
    assertThat(rest).hasSize(5);
    assertThat(((Map<String, Object>) rest.getLast()).get("shortCode"))
        .isEqualTo(links.getFirst().shortCode().value());
    assertThat(second.get("hasNext")).isEqualTo(false);
    assertThat(second.get("nextCursor")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "101", "abc", "-1", "1.5"})
  void rejectsAPageSizeOutsideOneToAHundred(String limit) throws Exception {
    mvc.perform(ownerGet(token(), "/api/v1/links?limit=" + limit))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("link.invalid_page_size"))
        .andExpect(
            jsonPath("$.detail").value("The page size must be a whole number from 1 to 100."));
  }

  @Test
  void rejectsAGarbledCursor() throws Exception {
    mvc.perform(ownerGet(token(), "/api/v1/links?after=garbage"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("link.invalid_cursor"))
        .andExpect(
            jsonPath("$.detail")
                .value("The continuation token is not valid. Start again from the first page."));
  }

  @Test
  void storageDownIsTemporarilyUnavailable() throws Exception {
    doThrow(new LinkStorageUnavailable(new RuntimeException()))
        .when(listShortLinks)
        .list(any(), any());

    mvc.perform(ownerGet(token(), "/api/v1/links"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("link.temporarily_unavailable"));
  }

  private Map<String, Object> page(String path) throws Exception {
    String body =
        mvc.perform(ownerGet(token(), path))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readValue(
        body, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }

  private ShortLink stored(ShortLink link) {
    store.insert(link);
    return link;
  }

  private String token() {
    return credential.ownerCredential();
  }
}
