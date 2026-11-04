package io.github.igorermolenko.trimly.redirect.api;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.app.LinkLookup;
import io.github.igorermolenko.trimly.link.app.Resolution;
import io.github.igorermolenko.trimly.link.domain.ShortLink;
import io.github.igorermolenko.trimly.link.infra.ShortLinkStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Visitors follow short links with no credential and get plain answers (AC-07…AC-19). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedirectIT {

  private static final String NOT_FOUND = "Link not found.";

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean ShortLinkStore store;
  @MockitoSpyBean LinkLookup lookup;

  private final HttpClient http =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/", "?utm_source=x", "/?utm_source=x&b=2"})
  void redirectsAnActiveLinkToItsExactTargetWithoutCredential(String suffix) throws Exception {
    ShortLink link = stored(aShortLink().withTargetUrl("https://example.test/A%20b?x=1#f").build());

    HttpResponse<String> response = get("/" + link.shortCode().value() + suffix);

    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().allValues("Location"))
        .containsExactly("https://example.test/A%20b?x=1#f");
    assertThat(response.headers().allValues("Cache-Control")).containsExactly("no-store");
    assertThat(response.body()).isEmpty();
  }

  @Test
  void aDisabledLinkLooksExactlyLikeAnUnknownCode() throws Exception {
    ShortLink disabled = stored(aShortLink().disabled().build());

    HttpResponse<String> forDisabled = get("/" + disabled.shortCode().value());
    HttpResponse<String> forUnknown = get("/zzzzzzz");

    assertNotFoundText(forDisabled);
    assertNotFoundText(forUnknown);
    assertThat(forDisabled.headers().map().keySet()).isEqualTo(forUnknown.headers().map().keySet());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/abc2345.", "/toolongcode", "/abc0345", "/abc2345/extra", "/a/b/c"})
  void aMalformedAddressIsNotFoundWithoutAStorageLookup(String path) throws Exception {
    clearInvocations(store);

    assertNotFoundText(get(path));
    verifyNoInteractions(store);
  }

  @Test
  void storageUnavailableIsTheTryAgainLaterSentence() throws Exception {
    doReturn(Resolution.Unavailable.INSTANCE).when(lookup).resolve(any());

    HttpResponse<String> response = get("/abc2345");

    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(response.body()).isEqualTo("Temporarily unavailable, try again later.");
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("text/plain"));
    assertThat(response.headers().allValues("Cache-Control")).containsExactly("no-store");
  }

  private void assertNotFoundText(HttpResponse<String> response) {
    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body()).isEqualTo(NOT_FOUND);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("text/plain"));
    assertThat(response.headers().allValues("Cache-Control")).containsExactly("no-store");
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private ShortLink stored(ShortLink link) {
    store.insert(link);
    return link;
  }
}
