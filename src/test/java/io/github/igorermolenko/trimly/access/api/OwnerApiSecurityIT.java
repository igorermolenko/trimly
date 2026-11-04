package io.github.igorermolenko.trimly.access.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** Only the owner credential opens the owner API; a missing and a wrong one look the same. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OwnerApiSecurityIT {

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;

  private final HttpClient http = HttpClient.newHttpClient();

  @Test
  void aMissingCredentialGetsTheAuthenticationRequiredProblem() throws Exception {
    HttpResponse<String> response = send("GET", "/api/v1/links", null);

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.headers().allValues("WWW-Authenticate")).containsExactly("Bearer");
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    assertThat(response.body())
        .contains("\"code\":\"access.authentication_required\"")
        .contains("\"status\":401")
        .contains("\"title\":\"Unauthorized\"")
        .contains("\"detail\":\"Authentication is required.\"")
        .doesNotContain("Exception");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Bearer wrong-credential",
        "Bearer",
        "Bearer ",
        "Basic b3duZXI6c2VjcmV0",
        "bearer test-owner-credential-Zq3!vR8#mK2$wT6%yH9&nB4x"
      })
  void aWrongCredentialGetsAByteIdenticalAnswer(String authorization) throws Exception {
    for (String method : List.of("GET", "POST")) {
      HttpResponse<String> missing = send(method, "/api/v1/links", null);
      HttpResponse<String> wrong = send(method, "/api/v1/links", authorization);

      assertThat(wrong.statusCode()).isEqualTo(401).isEqualTo(missing.statusCode());
      assertThat(wrong.body()).isEqualTo(missing.body());
      assertThat(comparable(wrong)).isEqualTo(comparable(missing));
    }
  }

  @Test
  void theOwnerCredentialPassesTheFilter() throws Exception {
    HttpResponse<String> response =
        send("GET", "/api/v1/no-such-endpoint", "Bearer " + credential.ownerCredential());

    assertThat(response.statusCode()).isNotEqualTo(401).isNotEqualTo(403);
    assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
  }

  @Test
  void visitorPathsNeedNoCredential() throws Exception {
    HttpResponse<String> response = send("GET", "/aB3dEf9", null);

    assertThat(response.statusCode()).isNotEqualTo(401).isNotEqualTo(403);
  }

  private HttpResponse<String> send(String method, String path, String authorization)
      throws Exception {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method(method, HttpRequest.BodyPublishers.noBody());
    if (authorization != null) {
      request.header("Authorization", authorization);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static Map<String, List<String>> comparable(HttpResponse<?> response) {
    Map<String, List<String>> headers = new TreeMap<>(response.headers().map());
    headers.remove("date");
    return headers;
  }
}
