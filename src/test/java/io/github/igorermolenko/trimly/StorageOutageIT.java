package io.github.igorermolenko.trimly;

import static io.github.igorermolenko.trimly.HttpTestClient.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** With link storage frozen, owners and visitors hear "temporarily unavailable" within 2 s. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(60)
class StorageOutageIT {

  private static final Duration BUDGET = Duration.ofSeconds(2);
  private static final Duration WAKE = Duration.ofMillis(1000);

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;
  @Autowired PostgreSQLContainer postgres;
  @Autowired JdbcTemplate jdbc;
  @Autowired HikariDataSource dataSource;

  private HttpTestClient client;
  private boolean paused;

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM short_link");
    client = new HttpTestClient(port, credential.ownerCredential());
  }

  @AfterEach
  synchronized void unpause() {
    if (paused) {
      DockerClientFactory.instance().client().unpauseContainerCmd(postgres.getContainerId()).exec();
      paused = false;
    }
  }

  @Test
  @DisplayName("AC-17b: every owner operation answers 503 Problem Details within 2 s")
  void ownerOperationsAreTemporarilyUnavailable() {
    String code = create();
    pause();

    Map<String, Supplier<HttpResponse<String>>> operations = new LinkedHashMap<>();
    operations.put(
        "create",
        () -> client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"https://example.test/\"}"));
    operations.put("list", () -> client.owner("GET", "/api/v1/links", null));
    operations.put(
        "disable", () -> client.owner("POST", "/api/v1/links/" + code + "/disable", null));
    operations.put("enable", () -> client.owner("POST", "/api/v1/links/" + code + "/enable", null));

    operations.forEach((name, operation) -> {
      HttpResponse<String> response = timed(name, operation);
      assertThat(response.statusCode()).as(name).isEqualTo(503);
      assertThat(response.headers().firstValue("Content-Type"))
          .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
      assertThat(json(response).get("code")).as(name).isEqualTo("link.temporarily_unavailable");
      assertThat(json(response).get("detail"))
          .isEqualTo("Temporarily unavailable, try again later.");
    });

    unpause();
    // AC-17b: once storage answers, the owner works normally; an unconfirmed create that did
    // commit would simply show up as a second link.
    HttpResponse<String> list = client.owner("GET", "/api/v1/links", null);
    assertThat(list.statusCode()).isEqualTo(200);
    assertThat((java.util.List<?>) json(list).get("items")).isNotEmpty();
    assertThat(client.owner("POST", "/api/v1/links/" + code + "/disable", null).statusCode())
        .isEqualTo(200);
  }

  @Test
  @DisplayName(
      "AC-17: a visitor gets the plain-text unavailable sentence within 2 s, then recovers")
  void visitorsAreToldToTryAgainLaterAndRecoverAtOnce() {
    String code = create();
    pause();

    HttpResponse<String> unavailable = timed("visit", () -> client.visit("/" + code));
    assertThat(unavailable.statusCode()).isEqualTo(503);
    assertThat(unavailable.body()).isEqualTo("Temporarily unavailable, try again later.");
    assertThat(unavailable.headers().firstValue("Content-Type"))
        .hasValueSatisfying(type -> assertThat(type).startsWith("text/plain"));
    assertThat(unavailable.headers().allValues("Cache-Control")).containsExactly("no-store");

    unpause();
    HttpResponse<String> recovered = client.visit("/" + code);
    assertThat(recovered.statusCode()).isEqualTo(302);
    assertThat(recovered.headers().allValues("Location")).containsExactly("https://example.test/");
  }

  @Test
  @DisplayName("AC-17: storage that wakes within 2 s still gives the visitor the normal redirect")
  void aVisitorArrivingDuringAWakeWithinTwoSecondsIsRedirected() throws Exception {
    String code = create();
    emptyThePool();
    pause();
    // Like a scaled-to-zero Neon: the new connection's login stalls until storage wakes up.
    Thread waker =
        Thread.ofVirtual()
            .start(() -> {
              sleepQuietly(WAKE);
              unpause();
            });

    HttpResponse<String> response = timed("visit during wake", () -> client.visit("/" + code));
    waker.join();

    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().allValues("Location")).containsExactly("https://example.test/");
    assertThat(response.headers().allValues("Cache-Control")).containsExactly("no-store");
  }

  @Test
  @DisplayName("AC-16b during AC-17: a malformed code is still 'link not found'")
  void aMalformedCodeIsStillNotFoundDuringTheOutage() {
    pause();

    HttpResponse<String> response = timed("malformed", () -> client.visit("/abc2345."));

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.body()).isEqualTo("Link not found.");
  }

  private String create() {
    HttpResponse<String> created =
        client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"https://example.test/\"}");
    assertThat(created.statusCode()).isEqualTo(201);
    return (String) json(created).get("shortCode");
  }

  private HttpResponse<String> timed(String name, Supplier<HttpResponse<String>> call) {
    long start = System.nanoTime();
    HttpResponse<String> response = call.get();
    assertThat(Duration.ofNanos(System.nanoTime() - start))
        .as(name + " took")
        .isLessThanOrEqualTo(BUDGET);
    return response;
  }

  private synchronized void pause() {
    DockerClientFactory.instance().client().pauseContainerCmd(postgres.getContainerId()).exec();
    paused = true;
  }

  private void emptyThePool() throws InterruptedException {
    dataSource.getHikariPoolMXBean().softEvictConnections();
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    while (dataSource.getHikariPoolMXBean().getTotalConnections() > 0
        && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertThat(dataSource.getHikariPoolMXBean().getTotalConnections()).isZero();
  }

  private static void sleepQuietly(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
