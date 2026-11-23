package hu.mik.pte.prog5.mozi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * A tesztek sajat, eldobhato PostgreSQL konteneren futnak, ugyanazokkal az init szkriptekkel,
 * mint a fejlesztoi adatbazis. A @ServiceConnection miatt a Spring Boot ehhez csatlakozik
 * az application.properties beallitasai helyett.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17")
                .withEnv("TZ", "Europe/Budapest")
                .withEnv("PGTZ", "Europe/Budapest")
                .withCopyFileToContainer(MountableFile.forHostPath("development/docker/init"),
                        "/docker-entrypoint-initdb.d/");
    }
}
