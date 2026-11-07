package io.github.igorermolenko.trimly.link.infra;

import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.NOW;
import static io.github.igorermolenko.trimly.link.ShortLinkFixtures.aShortLink;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.zaxxer.hikari.HikariDataSource;
import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.link.domain.LinkState;
import io.github.igorermolenko.trimly.link.domain.ShortCode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * With the link storage frozen, every store operation fails with a data-access failure that {@code
 * StorageFailures} reports as "storage unavailable", within the 2 s budget (ADR-0003); once it
 * answers again, the very next call succeeds.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Timeout(60)
class StorageTimeBudgetIT {

  private static final Duration BUDGET = Duration.ofSeconds(2);
  private static final Duration WAKE = Duration.ofMillis(1100);

  @Autowired ShortLinkStore store;
  @Autowired PostgreSQLContainer postgres;
  @Autowired HikariDataSource dataSource;

  @LocalManagementPort int managementPort;

  private boolean paused;

  @AfterEach
  synchronized void unpause() {
    if (paused) {
      docker().unpauseContainerCmd(postgres.getContainerId()).exec();
      paused = false;
    }
  }

  @Test
  void everyOperationAnswersUnavailableWithinTwoSecondsAndRecoversAtOnce() {
    store.insert(aShortLink().build());
    pause();

    Map<String, Executable> operations =
        Map.of(
            "insert", () -> store.insert(aShortLink().build()),
            "findByShortCode", () -> store.findByShortCode(ShortCode.of("abc2345")),
            "page", () -> store.page(null, 20),
            "setState", () -> store.setState(ShortCode.of("abc2345"), LinkState.DISABLED, NOW));
    operations.forEach((name, operation) -> {
      long start = System.nanoTime();
      Throwable failure = catchThrowable(operation::execute);
      Duration took = Duration.ofNanos(System.nanoTime() - start);

      assertThat(failure)
          .as(name)
          .isInstanceOf(DataAccessException.class)
          .isNotInstanceOf(DataIntegrityViolationException.class);
      assertThat(took).as(name + " took").isLessThanOrEqualTo(BUDGET);
    });

    unpause();
    assertThat(store.page(null, 20)).isNotEmpty();
  }

  @Test
  void aWakeThatAnswersWithinTwoSecondsIsServedNormally() throws Exception {
    store.insert(aShortLink().build());
    emptyThePool();
    pause();
    // Like a scaled-to-zero Neon: the new connection's login stalls until storage wakes up.
    Thread waker =
        Thread.ofVirtual()
            .start(() -> {
              sleepQuietly(WAKE);
              unpause();
            });

    long start = System.nanoTime();
    Throwable failure = catchThrowable(() -> assertThat(store.page(null, 20)).isNotEmpty());
    Duration took = Duration.ofNanos(System.nanoTime() - start);
    waker.join();

    assertThat(failure).as("a wake of %s within the 2 s budget", WAKE).isNull();
    assertThat(took).isLessThanOrEqualTo(BUDGET);
  }

  @Test
  void pooledConnectionsBoundEachStatementToTheStatementShare() throws Exception {
    try (var connection = dataSource.getConnection()) {
      assertThat(connection.getNetworkTimeout()).isEqualTo(600);
    }
    assertThat(dataSource.getConnectionTimeout()).isEqualTo(1400);
  }

  @Test
  void theIdlePoolDrains() {
    assertThat(dataSource.getMinimumIdle()).isZero();
    assertThat(dataSource.getIdleTimeout()).isLessThanOrEqualTo(Duration.ofSeconds(10).toMillis());
  }

  @Test
  void healthStaysUpWithoutTouchingStorage() throws Exception {
    pause();

    HttpResponse<String> response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(
                        URI.create("http://localhost:" + managementPort + "/actuator/health"))
                    .timeout(BUDGET)
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"UP\"").doesNotContain("\"db\"");
  }

  private synchronized void pause() {
    docker().pauseContainerCmd(postgres.getContainerId()).exec();
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

  private static com.github.dockerjava.api.DockerClient docker() {
    return DockerClientFactory.instance().client();
  }
}
