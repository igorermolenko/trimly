package io.github.igorermolenko.trimly;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app locally against a Testcontainers PostgreSQL: {@code ./mvnw spring-boot:test-run}.
 */
public class TestTrimlyApplication {

  public static void main(String[] args) {
    SpringApplication.from(TrimlyApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
