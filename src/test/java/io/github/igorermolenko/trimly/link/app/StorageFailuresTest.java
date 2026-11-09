package io.github.igorermolenko.trimly.link.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import io.github.igorermolenko.trimly.link.domain.ShortCodeTaken;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

class StorageFailuresTest {

  static Stream<DataAccessException> unavailable() {
    return Stream.of(
        new QueryTimeoutException("statement timeout"),
        new CannotGetJdbcConnectionException("pool timeout"),
        new TransientDataAccessResourceException("socket timeout"));
  }

  @ParameterizedTest
  @MethodSource("unavailable")
  void timeoutsAndConnectionFailuresBecomeLinkStorageUnavailable(DataAccessException failure) {
    assertThatThrownBy(() ->
            StorageFailures.call(() -> {
              throw failure;
            }))
        .isInstanceOf(LinkStorageUnavailable.class)
        .hasCause(failure);
  }

  @Test
  void aTakenShortCodePassesThrough() {
    ShortCodeTaken taken = new ShortCodeTaken();

    assertThatThrownBy(() ->
            StorageFailures.call(() -> {
              throw taken;
            }))
        .isSameAs(taken);
  }

  @Test
  void anotherConstraintViolationPropagatesUnchanged() {
    DataIntegrityViolationException bug = new DataIntegrityViolationException("check");

    assertThatThrownBy(() ->
            StorageFailures.call(() -> {
              throw bug;
            }))
        .isSameAs(bug);
  }

  @Test
  void returnsTheResultOfASuccessfulCall() {
    assertThat(StorageFailures.call(() -> 42)).isEqualTo(42);
  }
}
