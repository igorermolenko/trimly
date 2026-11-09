package io.github.igorermolenko.trimly.link.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.github.igorermolenko.trimly.HttpTestClient;
import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * AC-01, AC-09: real owner-API responses match the schemas in {@code contracts/openapi.yaml}, the
 * contract the feature was derived from, not a hand-copied field list.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OwnerApiContractIT {

  private static final Path CONTRACT =
      Path.of("docs/features/url-shortener/contracts/openapi.yaml");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static JsonNode contract;

  @LocalServerPort int port;
  @Autowired OwnerCredentialProperties credential;
  @Autowired JdbcTemplate jdbc;

  private HttpTestClient client;

  @BeforeAll
  static void loadContract() {
    contract = YAMLMapper.builder().build().readTree(CONTRACT.toFile());
  }

  @BeforeEach
  void setUp() {
    jdbc.update("DELETE FROM short_link");
    client = new HttpTestClient(port, credential.ownerCredential());
  }

  @Test
  void creatingALinkAnswersAShortLink() {
    HttpResponse<String> created = create("https://example.test/a");

    assertThat(created.statusCode()).isEqualTo(201);
    assertMatches(created, "ShortLink");
  }

  @Test
  void everyListPageIsAShortLinkPage() {
    create("https://example.test/a");
    create("https://example.test/b");

    HttpResponse<String> first = client.owner("GET", "/api/v1/links?limit=1", null);
    assertThat(first.statusCode()).isEqualTo(200);
    assertMatches(first, "ShortLinkPage");

    String cursor = HttpTestClient.json(first).get("nextCursor").toString();
    HttpResponse<String> last = client.owner("GET", "/api/v1/links?limit=1&after=" + cursor, null);
    assertThat(last.statusCode()).isEqualTo(200);
    assertMatches(last, "ShortLinkPage");
    assertThat(HttpTestClient.json(last).get("nextCursor")).isNull();
  }

  @Test
  void disablingAndEnablingAnswerAShortLinkState() {
    String code = (String) HttpTestClient.json(create("https://example.test/a")).get("shortCode");

    for (String action : List.of("disable", "enable")) {
      HttpResponse<String> changed =
          client.owner("POST", "/api/v1/links/" + code + "/" + action, null);
      assertThat(changed.statusCode()).as(action).isEqualTo(200);
      assertMatches(changed, "ShortLinkState");
    }
  }

  @Test
  void everyOwnerErrorIsAProblem() {
    List<HttpResponse<String>> problems =
        List.of(
            create("ftp://example.test/"),
            client.owner("POST", "/api/v1/links", "not json"),
            client.owner(
                "POST", "/api/v1/links", "{\"targetUrl\":\"https://example.test/\",\"x\":1}"),
            client.owner("GET", "/api/v1/links?limit=0", null),
            client.owner("POST", "/api/v1/links/zzzzzzz/disable", null),
            client.owner("DELETE", "/api/v1/links", null),
            client.withAuthorization("GET", "/api/v1/links", null, null),
            // Addresses Spring Security's firewall rejects before any controller runs.
            client.owner("POST", "/api/v1/links/ab%2Fcdef/disable", null),
            client.owner("POST", "/api/v1/links/abc2345;/enable", null));

    problems.forEach(problem -> {
      assertThat(problem.statusCode()).isBetween(400, 499);
      assertMatches(problem, "Problem");
    });
  }

  @Test
  void theOwnerApiRootIsAProblemNotAVisitorAnswer() {
    for (String path : List.of("/api", "/api/")) {
      HttpResponse<String> response = client.owner("GET", path, null);

      assertThat(response.statusCode()).as(path).isEqualTo(404);
      assertThat(response.headers().firstValue("Content-Type"))
          .as(path)
          .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
      assertMatches(response, "Problem");
      assertThat(HttpTestClient.json(response).get("code")).as(path).isEqualTo("shared.not_found");
    }
  }

  private HttpResponse<String> create(String targetUrl) {
    return client.owner("POST", "/api/v1/links", "{\"targetUrl\":\"" + targetUrl + "\"}");
  }

  private static void assertMatches(HttpResponse<String> response, String schemaName) {
    List<Error> errors = schema(schemaName).validate(JSON.readTree(response.body()));
    assertThat(errors).as("%s against %s", response.body(), schemaName).isEmpty();
  }

  /** The named component schema, with the contract's other components in scope for {@code $ref}. */
  private static Schema schema(String name) {
    assertThat(contract.at("/components/schemas/" + name).isMissingNode()).as(name).isFalse();
    ObjectNode root = JSON.createObjectNode();
    root.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    root.put("$ref", "#/components/schemas/" + name);
    root.set("components", contract.get("components"));
    return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(root);
  }
}
