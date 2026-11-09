package io.github.igorermolenko.trimly.link.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Owner-API requests carrying the test owner credential. */
final class OwnerApiTestSupport {

  private OwnerApiTestSupport() {}

  static MockHttpServletRequestBuilder ownerGet(String credential, String path, Object... vars) {
    return get(path, vars).header("Authorization", "Bearer " + credential);
  }

  static MockHttpServletRequestBuilder ownerPost(String credential, String path, Object... vars) {
    return post(path, vars).header("Authorization", "Bearer " + credential);
  }
}
