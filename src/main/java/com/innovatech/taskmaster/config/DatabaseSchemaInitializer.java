package com.innovatech.taskmaster.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String databaseProductName = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName()
        );

        if (databaseProductName == null || !databaseProductName.toLowerCase().contains("postgres")) {
            return;
        }

        try {
            jdbcTemplate.execute("ALTER TABLE usuario ALTER COLUMN foto_url TYPE TEXT");
        } catch (RuntimeException exception) {
            logger.warn("No se pudo ajustar usuario.foto_url a TEXT. Se continuara con el esquema actual.", exception);
        }
    }
}
