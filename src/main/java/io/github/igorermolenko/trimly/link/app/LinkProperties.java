package io.github.igorermolenko.trimly.link.app;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where trimly lives publicly; validated at startup, failing fast.
 *
 * @param publicBaseUrl the single public address every full short link is built from, whatever host
 *     a request arrived on (AC-01): absolute http/https, no trailing slash
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
        || publicBaseUrl.toString().endsWith("/")) {
      throw new IllegalArgumentException(
          "trimly.public-base-url must be an absolute http(s) URL without a trailing slash");
    }
    if (ownHosts == null || ownHosts.isEmpty()) {
      throw new IllegalArgumentException("trimly.own-hosts must name at least one host");
    }
    Set<String> hosts = new HashSet<>(ownHosts);
    hosts.add(publicBaseUrl.getHost());
    ownHosts = Set.copyOf(hosts);
  }

  /** The full short link for {@code shortCode}. */
  public String shortUrlFor(String shortCode) {
    return publicBaseUrl + "/" + shortCode;
  }
}
