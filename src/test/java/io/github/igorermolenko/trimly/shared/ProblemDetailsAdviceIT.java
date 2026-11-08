package io.github.igorermolenko.trimly.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.HttpTestClient;
import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

  // Tomcat cannot decode these queries; every parameter read then throws (review 2026-11-08-r2 S1).
  // A client sends them as typed, so the request target goes over a raw socket.
  @ParameterizedTest
  @ValueSource(strings = {"/api/v1/links?after=%zz", "/api/v1/links?limit=%", "/api/v1/links?a=%2"})
  void aQueryThatIsNotValidlyPercentEncodedIsAMalformedRequest(String target) throws Exception {
    String response = rawOwnerGet(target);

    String head = response.substring(0, response.indexOf("\r\n\r\n")).toLowerCase(Locale.ROOT);
    String body = response.substring(response.indexOf("\r\n\r\n") + 4);
    assertThat(head)
        .startsWith("http/1.1 400 ")
        .contains("\r\ncontent-type: application/problem+json")
        .contains("\r\ncache-control: no-cache, no-store, max-age=0, must-revalidate\r\n")
        .contains("\r\nx-content-type-options: nosniff\r\n");
    assertThat(body)
        .contains("\"code\":\"shared.malformed_request\"")
        .contains("\"status\":400")
        .doesNotContain("Exception")
        .doesNotContain("shared.internal_error");
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

  /** Sends the request target byte for byte as the owner; the whole response as text. */
  private String rawOwnerGet(String target) throws Exception {
    try (Socket socket = new Socket("localhost", port)) {
      OutputStream out = socket.getOutputStream();
      out.write(
          ("GET "
                  + target
                  + " HTTP/1.1\r\nHost: localhost\r\nAuthorization: Bearer "
                  + credential.ownerCredential()
                  + "\r\nConnection: close\r\n\r\n")
              .getBytes(StandardCharsets.ISO_8859_1));
      out.flush();
      ByteArrayOutputStream raw = new ByteArrayOutputStream();
      socket.getInputStream().transferTo(raw);
      return raw.toString(StandardCharsets.ISO_8859_1);
    }
  }
}
