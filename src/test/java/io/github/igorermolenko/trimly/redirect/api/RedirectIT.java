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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

  @ParameterizedTest
  @ValueSource(strings = {"Bearer wrong-credential", "Basic b3duZXI6c2VjcmV0"})
  void aVisitorWithAWrongCredentialIsStillRedirected(String authorization) throws Exception {
    ShortLink link = stored(aShortLink().withTargetUrl("https://example.test/x").build());

    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/" + link.shortCode().value()))
                .header("Authorization", authorization)
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().allValues("Location")).containsExactly("https://example.test/x");
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
  @ValueSource(
      strings = {
        "/",
        "/error",
        "/abc2345.",
        "/toolongcode",
        "/abc0345",
        "/abc2345/extra",
        "/a/b/c"
      })
  void aMalformedAddressIsNotFoundWithoutAStorageLookup(String path) throws Exception {
    clearInvocations(store);

    assertNotFoundText(get(path));
    verifyNoInteractions(store);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/abc2345;",
        "/abc2345;x=1",
        "/abc%25345",
        "/abc%2F2345",
        "//abc2345",
        "/abc2345//"
      })
  void anAddressTheFirewallRejectsIsStillNotFound(String path) throws Exception {
    clearInvocations(store);

    assertNotFoundText(get(path));
    verifyNoInteractions(store);
  }

  @Test
  void anOwnerApiAddressTheFirewallRejectsIsNotAVisitorAnswer() throws Exception {
    HttpResponse<String> response = get("/api/v1/links;x=1");

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    assertThat(response.body())
        .contains("\"code\":\"shared.malformed_request\"")
        .contains("\"type\":\"about:blank\"")
        .doesNotContain(NOT_FOUND)
        .doesNotContain("Exception")
        .doesNotContain("timestamp");
    // The same no-cache and nosniff headers as every other owner-API answer.
    assertThat(response.headers().allValues("Cache-Control"))
        .containsExactly("no-cache, no-store, max-age=0, must-revalidate");
    assertThat(response.headers().allValues("X-Content-Type-Options")).containsExactly("nosniff");
  }

  // Browsers send these characters unencoded in the query (and | [ ] ^ in the path), and
  // java.net.URI refuses them, so these cases go over a raw socket (review r4 Q1).
  @ParameterizedTest
  @ValueSource(strings = {"?a[]=1", "?q={x}", "?a=1|2", "?a=^", "?a=`b`", "/?a[0]=1"})
  void aQueryWithRawBrowserCharactersIsStillIgnored(String suffix) throws Exception {
    ShortLink link = stored(aShortLink().withTargetUrl("https://example.test/x").build());

    RawResponse response = rawGet("/" + link.shortCode().value() + suffix);

    assertThat(response.status()).isEqualTo(302);
    assertThat(response.headers("Location")).containsExactly("https://example.test/x");
    assertThat(response.headers("Cache-Control")).containsExactly("no-store");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/abc|234", "/abc[234", "/abc]234", "/abc^234", "/[abc2345]"})
  void aPathWithRawBrowserCharactersIsNotFoundWithoutAStorageLookup(String path) throws Exception {
    clearInvocations(store);

    RawResponse response = rawGet(path);

    assertThat(response.status()).isEqualTo(404);
    assertThat(response.body()).isEqualTo(NOT_FOUND);
    assertThat(response.headers("Content-Type"))
        .singleElement()
        .satisfies(type -> assertThat(type).startsWith("text/plain"));
    assertThat(response.headers("Cache-Control")).containsExactly("no-store");
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

  private record RawResponse(int status, List<String[]> headerLines, String body) {
    List<String> headers(String name) {
      return headerLines.stream().filter(h -> h[0].equalsIgnoreCase(name)).map(h -> h[1]).toList();
    }
  }

  /** Sends the request target byte for byte, without any client-side validation or encoding. */
  private RawResponse rawGet(String target) throws Exception {
    try (Socket socket = new Socket("localhost", port)) {
      OutputStream out = socket.getOutputStream();
      out.write(
          ("GET " + target + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
              .getBytes(StandardCharsets.ISO_8859_1));
      out.flush();
      InputStream in = socket.getInputStream();
      ByteArrayOutputStream raw = new ByteArrayOutputStream();
      in.transferTo(raw);
      String text = raw.toString(StandardCharsets.ISO_8859_1);
      int split = text.indexOf("\r\n\r\n");
      String[] head = text.substring(0, split).split("\r\n");
      List<String[]> headers = new ArrayList<>();
      boolean chunked = false;
      for (int i = 1; i < head.length; i++) {
        int colon = head[i].indexOf(':');
        String name = head[i].substring(0, colon).trim();
        String value = head[i].substring(colon + 1).trim();
        headers.add(new String[] {name, value});
        chunked |=
            name.equalsIgnoreCase("Transfer-Encoding")
                && value.toLowerCase(Locale.ROOT).contains("chunked");
      }
      String body = text.substring(split + 4);
      return new RawResponse(
          Integer.parseInt(head[0].split(" ")[1]), headers, chunked ? unchunk(body) : body);
    }
  }

  private static String unchunk(String body) {
    StringBuilder out = new StringBuilder();
    int at = 0;
    while (true) {
      int lineEnd = body.indexOf("\r\n", at);
      int size = Integer.parseInt(body.substring(at, lineEnd).trim(), 16);
      if (size == 0) {
        return out.toString();
      }
      out.append(body, lineEnd + 2, lineEnd + 2 + size);
      at = lineEnd + 2 + size + 2;
    }
  }
}
