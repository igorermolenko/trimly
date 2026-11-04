package io.github.igorermolenko.trimly.link.app;

import io.github.igorermolenko.trimly.link.domain.LinkStorageUnavailable;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Turns link-storage failures into an explicit "storage unavailable" outcome (ADR-0003). Integrity
 * violations are not storage failures: they mean a bug and propagate unchanged.
 */
final class StorageFailures {

  private static final Logger log = LoggerFactory.getLogger(StorageFailures.class);

  private StorageFailures() {}

  static <T> T call(Supplier<T> storageCall) {
    try {
      return storageCall.get();
    } catch (DataIntegrityViolationException e) {
      throw e;
    } catch (DataAccessException e) {
      log.warn("Link storage unavailable: {}", e.getClass().getSimpleName());
      throw new LinkStorageUnavailable(e);
    }
  }

  static void run(Runnable storageCall) {
    call(() -> {
      storageCall.run();
      return null;
    });
  }
}
