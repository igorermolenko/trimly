package io.github.igorermolenko.trimly.access.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** AC-18: only the exact owner credential authenticates; anything close to it does not. */
class OwnerCredentialFilterTest {

  private static final String CREDENTIAL = "test-owner-credential-Zq3!vR8#mK2$wT6%yH9&nB4";

  private final OwnerCredentialFilter filter = new OwnerCredentialFilter(CREDENTIAL);

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void theExactCredentialAuthenticatesTheOwner() throws Exception {
    assertThat(authenticates("Bearer " + CREDENTIAL)).isTrue();
  }

  @ParameterizedTest
  @MethodSource("nearMisses")
  void anythingButTheExactCredentialIsRejected(String presented) throws Exception {
    assertThat(authenticates("Bearer " + presented)).as(presented).isFalse();
  }

  static String[] nearMisses() {
    return new String[] {
      CREDENTIAL.substring(0, CREDENTIAL.length() - 1),
      CREDENTIAL + "x",
      CREDENTIAL.toUpperCase(Locale.ROOT),
      CREDENTIAL.toLowerCase(Locale.ROOT),
      CREDENTIAL + " ",
      " " + CREDENTIAL,
      ""
    };
  }

  private boolean authenticates(String authorization) throws Exception {
    SecurityContextHolder.clearContext();
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/links");
    request.addHeader("Authorization", authorization);

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    return SecurityContextHolder.getContext().getAuthentication() != null;
  }
}
