package io.github.igorermolenko.trimly;

import static io.github.igorermolenko.trimly.HttpTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** The sad §6 cross-cutting link lifecycle, owner and visitor over real HTTP. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LinkLifecycleIT {

  private static final String X = "https://example.test/articles/1?utm_source=newsletter";

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;
  @Autowired JdbcTemplate jdbc;

  private HttpTestClient client;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM short_link");
    client = new HttpTestClient(port, credential.ownerCredential());
  }

  @Test
  @DisplayName(
      "AC-02, AC-08, AC-12, AC-15, AC-20: create, follow, create again, disable, re-enable")
  void aLinkKeepsItsCodeAndTargetThroughItsWholeLife() {
    // O creates X -> A; V opens A at once -> redirect to X, not reusable (AC-08).
    String a = create(X);
    assertRedirectsTo(a, X);

    // O creates X again -> B, a separate link; A unchanged (AC-02).
    String b = create(X);
    assertThat(b).isNotEqualTo(a);
    assertRedirectsTo(a, X);

    // O disables A; V opens A again -> "link not found" (AC-12).
    assertThat(client.owner("POST", "/api/v1/links/" + a + "/disable", null).statusCode())
        .isEqualTo(200);
    HttpResponse<String> disabled = client.visit("/" + a);
    assertThat(disabled.statusCode()).isEqualTo(404);
    assertThat(disabled.body()).isEqualTo("Link not found.");
    assertRedirectsTo(b, X);

    // O re-enables A; V opens A -> the same X under the same code (AC-15, AC-20).
    assertThat(client.owner("POST", "/api/v1/links/" + a + "/enable", null).statusCode())
        .isEqualTo(200);
    assertRedirectsTo(a, X);

    // Postcondition: A and B both exist, both lead to X, and the list shows both (AC-02, AC-20).
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items =
        (List<Map<String, Object>>) json(client.owner("GET", "/api/v1/links", null)).get("items");
    assertThat(items)
        .extracting(item -> (Object) item.get("shortCode"))
        .containsExactly((Object) b, a);
    assertThat(items).allSatisfy(item -> assertThat(item.get("targetUrl")).isEqualTo(X));
  }

  private String create(String target) {
    HttpResponse<String> response =
        client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"" + target + "\"}");
    assertThat(response.statusCode()).isEqualTo(201);
    return (String) json(response).get("shortCode");
  }

  private void assertRedirectsTo(String code, String target) {
    HttpResponse<String> response = client.visit("/" + code);
    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().allValues("Location")).containsExactly(target);
    assertThat(response.headers().allValues("Cache-Control")).containsExactly("no-store");
  }
}
