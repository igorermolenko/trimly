package io.github.igorermolenko.trimly.link.domain;

import io.github.igorermolenko.trimly.link.domain.TargetUrlRejected.Reason;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * A target URL that may become a short link. Its value is exactly the submitted text: it is never
 * trimmed, normalised or re-encoded (AC-01, AC-05).
 */
public record TargetUrl(String value) {

  public static final int MAX_LENGTH = 2048;

  /**
   * Applies the target-URL rules in order: present, at most {@value #MAX_LENGTH} characters, only
   * printable ASCII, an absolute http/https address with a host, and not one of {@code ownHosts} or
   * a subdomain of one.
   *
   * @throws TargetUrlRejected naming the first rule the text breaks
   */
  public static TargetUrl parse(String raw, Set<String> ownHosts) {
    if (raw == null || raw.isEmpty()) {
      throw new TargetUrlRejected(Reason.UNSUPPORTED);
    }
    if (raw.length() > MAX_LENGTH) {
      throw new TargetUrlRejected(Reason.TOO_LONG);
    }
    if (!raw.chars().allMatch(c -> c >= 0x21 && c <= 0x7E)) {
      throw new TargetUrlRejected(Reason.NOT_ENCODED);
    }
    String host = webHost(raw);
    if (pointsTo(host, ownHosts)) {
      throw new TargetUrlRejected(Reason.POINTS_TO_TRIMLY);
    }
    return new TargetUrl(raw);
  }

  private static String webHost(String raw) {
    URI uri;
    try {
      uri = new URI(raw);
    } catch (URISyntaxException e) {
      throw new TargetUrlRejected(Reason.UNSUPPORTED);
    }
    String scheme = uri.getScheme();
    boolean web = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    if (!web || uri.isOpaque() || uri.getHost() == null || uri.getHost().isEmpty()) {
      throw new TargetUrlRejected(Reason.UNSUPPORTED);
    }
    return uri.getHost();
  }

  private static boolean pointsTo(String host, Set<String> ownHosts) {
    String candidate = canonical(host);
    return ownHosts.stream()
        .map(TargetUrl::canonical)
        .anyMatch(own -> candidate.equals(own) || candidate.endsWith("." + own));
  }

  private static String canonical(String host) {
    String lower = host.toLowerCase(Locale.ROOT);
    return lower.endsWith(".") ? lower.substring(0, lower.length() - 1) : lower;
  }
}
