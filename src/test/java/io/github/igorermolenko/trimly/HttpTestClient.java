package io.github.igorermolenko.trimly;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Real-HTTP calls against the running app; redirects are never followed. */
public final class HttpTestClient {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient http =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  private final int port;
  private final String ownerCredential;

  public HttpTestClient(int port, String ownerCredential) {
    this.port = port;
    this.ownerCredential = ownerCredential;
  }

  /** A visitor request: no credential. */
  public HttpResponse<String> visit(String path) {
    return send(HttpRequest.newBuilder(uri(path)).GET());
  }

  /** An owner request with the owner credential. */
  public HttpResponse<String> owner(String method, String path, String jsonBody) {
    return withAuthorization(method, path, jsonBody, "Bearer " + ownerCredential);
  }

  /** A request with the given {@code Authorization} header, or none when null. */
  public HttpResponse<String> withAuthorization(
      String method, String path, String jsonBody, String authorization) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(uri(path))
            .method(
                method,
                jsonBody == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(jsonBody))
            .header("Content-Type", "application/json");
    if (authorization != null) {
      request.header("Authorization", authorization);
    }
    return send(request);
  }

  public static Map<String, Object> json(HttpResponse<String> response) {
    return JSON.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
  }

  /** Response headers without the ones that legitimately differ between two calls. */
  public static Map<String, List<String>> comparableHeaders(HttpResponse<?> response) {
    Map<String, List<String>> headers = new TreeMap<>(response.headers().map());
    headers.remove("date");
    return headers;
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private HttpResponse<String> send(HttpRequest.Builder request) {
    try {
      return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
