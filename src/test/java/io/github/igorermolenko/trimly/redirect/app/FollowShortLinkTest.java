package io.github.igorermolenko.trimly.redirect.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.igorermolenko.trimly.link.app.LinkLookup;
import io.github.igorermolenko.trimly.link.app.Resolution;
import org.junit.jupiter.api.Test;

class FollowShortLinkTest {

  private final LinkLookup lookup = mock(LinkLookup.class);
  private final FollowShortLink follow = new FollowShortLink(lookup);

  @Test
  void aFoundLinkIsATemporaryRedirectToTheExactTarget() {
    when(lookup.resolve("abc2345")).thenReturn(new Resolution.Found("https://example.test/A?b=c"));

    assertThat(follow.follow("abc2345"))
        .isEqualTo(new VisitorAnswer.Redirect("https://example.test/A?b=c"));
  }

  @Test
  void notFoundIsTheLinkNotFoundSentence() {
    when(lookup.resolve("abc2345")).thenReturn(Resolution.NotFound.INSTANCE);

    assertThat(follow.follow("abc2345")).isEqualTo(VisitorAnswer.Message.NOT_FOUND);
    assertThat(VisitorAnswer.Message.NOT_FOUND.text()).isEqualTo("Link not found.");
  }

  @Test
  void unavailableIsTheTryAgainLaterSentence() {
    when(lookup.resolve("abc2345")).thenReturn(Resolution.Unavailable.INSTANCE);

    assertThat(follow.follow("abc2345")).isEqualTo(VisitorAnswer.Message.UNAVAILABLE);
    assertThat(VisitorAnswer.Message.UNAVAILABLE.text())
        .isEqualTo("Temporarily unavailable, try again later.");
  }
}
