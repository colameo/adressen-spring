package ch.example.adressen.config;

import java.sql.SQLException;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lets a database tool (IntelliJ, DBeaver) connect to the in-memory H2 while the application runs:
 * {@code jdbc:h2:tcp://localhost:<h2.tcp.port>/mem:<name>}, user and password as in application.yml.
 * Only reachable from this machine. Switch it off with {@code h2.tcp.enabled: false}.
 */
@Configuration
@ConditionalOnProperty(prefix = "h2.tcp", name = "enabled", havingValue = "true")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer(@Value("${h2.tcp.port:9092}") String port) throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpPort", port, "-ifNotExists");
    }
}
