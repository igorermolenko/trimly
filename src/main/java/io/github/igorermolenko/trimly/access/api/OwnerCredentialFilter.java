package io.github.igorermolenko.trimly.access.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the owner when the request carries {@code Authorization: Bearer <owner
 * credential>}. Anything else leaves the request unauthenticated, so a missing and a wrong
 * credential reach the same entry point (AC-18). The header is never logged.
 */
final class OwnerCredentialFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";

  private final byte[] expectedDigest;

  OwnerCredentialFilter(String ownerCredential) {
    this.expectedDigest = sha256(ownerCredential);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization != null
        && authorization.startsWith(BEARER)
        // Compare fixed-length digests in constant time, so timing reveals neither content nor
        // length.
        && MessageDigest.isEqual(
            expectedDigest, sha256(authorization.substring(BEARER.length())))) {
      SecurityContextHolder.getContext()
          .setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated(
                  "owner", null, List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
    }
    chain.doFilter(request, response);
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
