package io.github.igorermolenko.trimly;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** Skeleton smoke test: the full context boots against PostgreSQL with all migrations applied. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationBootIT {

  @Autowired Flyway flyway;
  @Autowired OwnerCredentialProperties credential;

  @LocalServerPort int port;

  @Test
  void appliesAllMigrationsOnStartup() {
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
  }

  @Test
  void rerunningMigrationsIsANoOp() {
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void errorsAreProblemDetails() throws Exception {
    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/no-such-path"))
                    .header("Authorization", "Bearer " + credential.ownerCredential())
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    assertThat(response.body()).doesNotContain("Exception");
  }
}
