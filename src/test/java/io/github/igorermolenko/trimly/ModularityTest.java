package io.github.igorermolenko.trimly;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails the build when a module reaches into another module's internals or forms a cycle. */
class ModularityTest {

  @Test
  void moduleBoundariesAreRespected() {
    ApplicationModules.of(TrimlyApplication.class).verify();
  }
}
