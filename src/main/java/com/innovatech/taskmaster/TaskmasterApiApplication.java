package com.innovatech.taskmaster;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskmasterApiApplication {

    public static void main(String[] args) {
        applyRenderDatabaseSettings();
        SpringApplication.run(TaskmasterApiApplication.class, args);
    }

    private static void applyRenderDatabaseSettings() {
        String databaseUrl = firstNonBlank(
            System.getenv("DATABASE_URL"),
            System.getenv("SPRING_DATASOURCE_URL")
        );

        if (databaseUrl == null || !databaseUrl.startsWith("postgresql://")) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String databaseName = uri.getPath() != null ? uri.getPath().replaceFirst("^/", "") : "";
            String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];

            if (host != null && !databaseName.isBlank()) {
                System.setProperty("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + databaseName);
            }

            if (userInfo.length > 0 && !userInfo[0].isBlank()) {
                System.setProperty("spring.datasource.username", userInfo[0]);
            }

            if (userInfo.length > 1 && !userInfo[1].isBlank()) {
                System.setProperty("spring.datasource.password", userInfo[1]);
            }
        } catch (URISyntaxException ignored) {
            // If the connection string is malformed, Spring will surface the startup error.
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
