package io.github.igorermolenko.trimly.link.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.igorermolenko.trimly.TestcontainersConfiguration;
import io.github.igorermolenko.trimly.access.app.OwnerCredentialProperties;
import io.github.igorermolenko.trimly.link.app.CreateShortLink;
import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = {
      "trimly.public-base-url=https://trimly.example.test",
      "trimly.own-hosts=trimly.example.test"
    })
@AutoConfigureMockMvc
class CreateShortLinkIT {

  @Autowired MockMvc mvc;
  @Autowired OwnerCredentialProperties credential;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean CreateShortLink createShortLink;

  private final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM short_link");
  }

  @Test
  void createsAShortLinkWithTheShortUrlFromThePublicBaseUrl() throws Exception {
    String target = "https://example.test/a?utm_source=x&b=%20#frag";

    create(json.writeValueAsString(java.util.Map.of("targetUrl", target)), "other.example.test")
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.shortCode").value(matchesPattern("^[A-HJ-NP-Za-km-np-z2-9]{7}$")))
        .andExpect(
            jsonPath("$.shortUrl")
                .value(
                    matchesPattern("^https://trimly\\.example\\.test/[A-HJ-NP-Za-km-np-z2-9]{7}$")))
        .andExpect(jsonPath("$.targetUrl").value(target))
        .andExpect(jsonPath("$.state").value("ACTIVE"))
        .andExpect(jsonPath("$.createdAt").value(matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$")));

    assertThat(storedLinks()).isEqualTo(1);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"targetUrl\":\"ftp://example.test/\"}|link.target_url_unsupported|Only absolute http or https web addresses are accepted.",
        "{\"targetUrl\":\"\"}|link.target_url_unsupported|Only absolute http or https web addresses are accepted.",
        "{}|link.target_url_unsupported|Only absolute http or https web addresses are accepted.",
        "{\"targetUrl\":\" https://example.test/\"}|link.target_url_not_encoded|The target URL must be properly encoded: non-Latin hosts in their ASCII xn-- form, other characters percent-encoded.",
        "{\"targetUrl\":\"https://trimly.example.test/abc2345\"}|link.target_url_points_to_trimly|A short link cannot point back to trimly.",
        "[]|shared.malformed_request|",
        "not json|shared.malformed_request|"
      })
  void rejectsABadRequestWithItsProblemCode(String body, String code, String detail)
      throws Exception {
    ResultActions result =
        create(body, "localhost")
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value(code));
    if (detail != null) {
      result.andExpect(jsonPath("$.detail").value(detail));
    }
    assertThat(result.andReturn().getResponse().getContentAsString())
        .doesNotContain("Exception")
        .doesNotContain("TargetUrlRejected");
    assertThat(storedLinks()).isZero();
  }

  @Test
  void rejectsATooLongTargetUrl() throws Exception {
    String target = "https://example.test/" + "a".repeat(2048);

    create(json.writeValueAsString(java.util.Map.of("targetUrl", target)), "localhost")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("link.target_url_too_long"))
        .andExpect(
            jsonPath("$.detail").value("The target URL must be at most 2048 characters long."));
  }

  @Test
  void storageUnavailableIsTemporarilyUnavailable() throws Exception {
    doThrow(new LinkStorageUnavailable(new RuntimeException("secret detail")))
        .when(createShortLink)
        .create(any());

    String body =
        create("{\"targetUrl\":\"https://example.test/\"}", "localhost")
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("link.temporarily_unavailable"))
            .andExpect(jsonPath("$.detail").value("Temporarily unavailable, try again later."))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("secret detail");
  }

  private ResultActions create(String body, String host) throws Exception {
    return mvc.perform(
        post("/api/v1/links")
            .with(request -> {
              request.setServerName(host);
              return request;
            })
            .header("Authorization", "Bearer " + credential.ownerCredential())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private int storedLinks() {
    return jdbc.queryForObject("SELECT count(*) FROM short_link", Integer.class);
  }
}
