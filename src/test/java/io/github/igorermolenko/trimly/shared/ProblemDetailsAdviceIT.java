package io.github.igorermolenko.trimly.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.HttpTestClient;
import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Every Problem Details answer on the owner API carries a {@code code} (openapi Problem). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProblemDetailsAdviceIT {

  private static final String CREATE_BODY = "{\"targetUrl\":\"https://example.test/\"}";

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;
  @Autowired JdbcTemplate jdbc;

  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"text/plain", "application/x-www-form-urlencoded"})
  void aCreateBodyThatIsNotJsonIsAMalformedRequest(String contentType) throws Exception {
    HttpRequest.Builder request =
        owner("/api/v1/links").POST(HttpRequest.BodyPublishers.ofString(CREATE_BODY));
    if (contentType != null) {
      request.header("Content-Type", contentType);
    }

    HttpResponse<String> response = send(request);

    assertProblem(response, 400, "shared.malformed_request");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM short_link", Integer.class)).isZero();
  }

  @Test
  void anUnknownOwnerApiAddressIsNotFound() throws Exception {
    assertProblem(send(owner("/api/v1/nope/x").GET()), 404, "shared.not_found");
  }

  @Test
  void anUnsupportedMethodIsNotAllowed() throws Exception {
    assertProblem(send(owner("/api/v1/links").DELETE()), 405, "shared.method_not_allowed");
  }

  @Test
  void anUnacceptableResponseTypeIsNotAcceptable() throws Exception {
    assertProblem(
        send(owner("/api/v1/links").header("Accept", "text/csv").GET()),
        406,
        "shared.not_acceptable");
  }

  private void assertProblem(HttpResponse<String> response, int status, String code) {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    Map<String, Object> problem = HttpTestClient.json(response);
    assertThat(problem).containsEntry("status", status).containsEntry("code", code);
    assertThat(response.body()).doesNotContain("Exception");
  }

  private HttpRequest.Builder owner(String path) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("Authorization", "Bearer " + credential.ownerCredential());
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
