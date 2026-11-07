package io.github.igorermolenko.trimly.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class ProblemDetailsAdviceTest {

  @Test
  void anUnexpectedFailureIsAnInternalErrorWithACodeAndNoMessage() {
    ProblemDetail problem =
        new ProblemDetailsAdvice().handleUnexpected(new IllegalStateException("secret detail"));

    assertThat(problem.getStatus()).isEqualTo(500);
    assertThat(problem.getProperties()).containsEntry("code", "shared.internal_error");
    assertThat(problem.getDetail()).isNull();
  }
}
