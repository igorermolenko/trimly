package io.github.igorermolenko.trimly.link.app;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where trimly lives publicly; validated at startup, failing fast.
 *
 * @param publicBaseUrl the single public address every full short link is built from, whatever host
 *     a request arrived on (AC-01): absolute http/https with only a host and an optional port
 * @param ownHosts trimly's own hosts, which target URLs may not point to (AC-06); always includes
 *     the public base URL's host, so a short link can never point at another short link
 */
@ConfigurationProperties("trimly")
public record LinkProperties(URI publicBaseUrl, Set<String> ownHosts) {

  public LinkProperties {
    if (publicBaseUrl == null
        || !publicBaseUrl.isAbsolute()
        || !("http".equals(publicBaseUrl.getScheme()) || "https".equals(publicBaseUrl.getScheme()))
        || publicBaseUrl.getHost() == null
        || publicBaseUrl.getRawUserInfo() != null
        || !publicBaseUrl.getRawPath().isEmpty()
        || publicBaseUrl.getRawQuery() != null
        || publicBaseUrl.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "trimly.public-base-url must be an absolute http(s) URL with only a host and an optional"
              + " port: no path, trailing slash, query, fragment or user info");
    }
    if (ownHosts == null || ownHosts.isEmpty()) {
      throw new IllegalArgumentException("trimly.own-hosts must name at least one host");
    }
    for (String host : ownHosts) {
      if (!isBareHost(host)) {
        throw new IllegalArgumentException(
            "trimly.own-hosts entries must be bare host names, without scheme, port or path");
      }
    }
    Set<String> hosts = new HashSet<>(ownHosts);
    hosts.add(publicBaseUrl.getHost());
    ownHosts = Set.copyOf(hosts);
  }

  private static boolean isBareHost(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    try {
      URI parsed = new URI("http://" + host);
      return host.equalsIgnoreCase(parsed.getHost())
          && parsed.getPort() == -1
          && parsed.getRawPath().isEmpty()
          && parsed.getRawQuery() == null
          && parsed.getRawFragment() == null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /** The full short link for {@code shortCode}. */
  public String shortUrlFor(String shortCode) {
    return publicBaseUrl + "/" + shortCode;
  }
}
