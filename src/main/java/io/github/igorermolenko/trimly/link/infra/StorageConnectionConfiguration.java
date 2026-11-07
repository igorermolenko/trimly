package io.github.igorermolenko.trimly.link.infra;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.DriverDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Splits the 2 s link-storage budget between opening a connection and using it (ADR-0003).
 *
 * <p>The PostgreSQL driver applies its whole-second {@code socketTimeout} to the login as well, and
 * a Neon wake-up happens during the login. So the driver timeouts stay long enough for a wake (see
 * {@code application.yml}), and each new connection's network timeout is lowered to {@code
 * trimly.storage.statement-network-timeout} right after login. Hikari reads that value from the
 * connection and restores it after every validation, so it bounds every statement on a storage that
 * stops answering.
 */
@Configuration(proxyBeanMethods = false)
class StorageConnectionConfiguration {

  static final String STATEMENT_NETWORK_TIMEOUT = "trimly.storage.statement-network-timeout";

  @Bean
  static BeanPostProcessor statementNetworkTimeout(Environment environment) {
    Duration timeout =
        Binder.get(environment)
            .bind(STATEMENT_NETWORK_TIMEOUT, Duration.class)
            .orElseThrow(
                () -> new IllegalStateException(STATEMENT_NETWORK_TIMEOUT + " is not set"));
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof HikariDataSource hikari && hikari.getDataSource() == null) {
          DataSource driver =
              new DriverDataSource(
                  hikari.getJdbcUrl(),
                  hikari.getDriverClassName(),
                  hikari.getDataSourceProperties(),
                  hikari.getUsername(),
                  hikari.getPassword());
          hikari.setDataSource(new StatementNetworkTimeout(driver, timeout));
        }
        return bean;
      }
    };
  }

  /**
   * Migrations are not link-storage calls: they run on their own driver connection to the same
   * database, without the request budget's statement or network timeouts, so a long migration, a
   * wait for another instance's migration lock or a slow wake never stops trimly from starting.
   */
  @Bean
  FlywayConfigurationCustomizer migrationsOutsideTheRequestBudget(HikariDataSource storage) {
    return configuration ->
        configuration.dataSource(
            storage.getJdbcUrl(), storage.getUsername(), storage.getPassword());
  }

  /** Lowers the network timeout of every connection it opens, once the login is done. */
  static final class StatementNetworkTimeout extends DelegatingDataSource {

    private final int millis;

    StatementNetworkTimeout(DataSource target, Duration timeout) {
      super(target);
      this.millis = Math.toIntExact(timeout.toMillis());
    }

    @Override
    public Connection getConnection() throws SQLException {
      return bounded(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
      return bounded(super.getConnection(username, password));
    }

    private Connection bounded(Connection connection) throws SQLException {
      try {
        connection.setNetworkTimeout(Runnable::run, millis);
        return connection;
      } catch (SQLException e) {
        connection.close();
        throw e;
      }
    }
  }
}
