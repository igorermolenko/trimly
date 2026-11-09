package io.github.igorermolenko.trimly;

import static io.github.igorermolenko.trimly.HttpTestClient.comparableHeaders;
import static io.github.igorermolenko.trimly.HttpTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** A missing and a wrong owner credential are indistinguishable and change nothing (AC-18). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class OwnerAuthDenialIT {

  private static final String WRONG = "not-the-owner-credential";
  private static final String BODY = "{\"targetUrl\":\"https://example.test/denied\"}";

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;
  @Autowired JdbcTemplate jdbc;

  private HttpTestClient client;
  private String code;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM short_link");
    client = new HttpTestClient(port, credential.ownerCredential());
    HttpResponse<String> created =
        client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"https://example.test/\"}");
    code = (String) json(created).get("shortCode");
  }

  @Test
  @DisplayName("AC-18: missing and wrong credentials get byte-identical denials on all four ops")
  void missingAndWrongCredentialsAreIndistinguishableAndChangeNothing() {
    String rowsBefore = rows();
    for (String[] op : operations()) {
      HttpResponse<String> missing = client.withAuthorization(op[0], op[1], op[2], null);
      HttpResponse<String> wrong = client.withAuthorization(op[0], op[1], op[2], "Bearer " + WRONG);

      assertThat(missing.statusCode()).as(op[1]).isEqualTo(401);
      assertThat(wrong.statusCode()).as(op[1]).isEqualTo(401);
      assertThat(wrong.body()).as(op[1]).isEqualTo(missing.body());
      assertThat(comparableHeaders(wrong)).as(op[1]).isEqualTo(comparableHeaders(missing));
      // No link data: the denial for an existing code equals the one for a code that does not
      // exist (only the echoed request path differs), and never carries a target URL.
      HttpResponse<String> forNoSuchCode =
          client.withAuthorization(op[0], op[1].replace(code, "zzzzzzz"), op[2], null);
      assertThat(missing.body().replace(code, "zzzzzzz")).as(op[1]).isEqualTo(forNoSuchCode.body());
      assertThat(missing.body()).doesNotContain("example.test");
    }
    assertThat(rows()).isEqualTo(rowsBefore);
  }

  @Test
  @DisplayName("AC-18: the denial has the same Problem Details shape as the shared advice's 400")
  void theDenialHasTheSharedProblemDetailsShape() {
    HttpResponse<String> denied = client.withAuthorization("POST", "/api/v1/links", BODY, null);
    HttpResponse<String> badRequest =
        client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"ftp://example.test/\"}");

    assertThat(badRequest.statusCode()).isEqualTo(400);
    assertThat(denied.headers().firstValue("Content-Type"))
        .isEqualTo(badRequest.headers().firstValue("Content-Type"));
    assertThat(json(denied).keySet()).isEqualTo(json(badRequest).keySet());
    assertThat(json(denied).get("code")).isEqualTo("access.authentication_required");
  }

  @Test
  @DisplayName("AC-18 / spec §6.1: neither the owner credential nor a wrong one is ever logged")
  void noCredentialIsEverWrittenToTheLogs(CapturedOutput output) {
    for (String[] op : operations()) {
      client.owner(op[0], op[1], op[2]);
      client.withAuthorization(op[0], op[1], op[2], "Bearer " + WRONG);
      client.withAuthorization(op[0], op[1], op[2], null);
    }

    assertThat(output.getAll()).doesNotContain(credential.ownerCredential()).doesNotContain(WRONG);
  }

  private List<String[]> operations() {
    return List.of(
        new String[] {"POST", "/api/v1/links", BODY},
        new String[] {"GET", "/api/v1/links", null},
        new String[] {"POST", "/api/v1/links/" + code + "/disable", null},
        new String[] {"POST", "/api/v1/links/" + code + "/enable", null});
  }

  private String rows() {
    return jdbc.queryForList(
            "SELECT short_code || ':' || state || ':' || updated_at FROM short_link ORDER BY id",
            String.class)
        .toString();
  }
}
