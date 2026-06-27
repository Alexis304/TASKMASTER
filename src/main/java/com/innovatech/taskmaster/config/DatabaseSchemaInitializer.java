package com.innovatech.taskmaster.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean cleanupDemoData;

    public DatabaseSchemaInitializer(
        JdbcTemplate jdbcTemplate,
        @Value("${taskmaster.cleanup.demo-data:true}") boolean cleanupDemoData
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.cleanupDemoData = cleanupDemoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        String databaseProductName = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName()
        );

        if (databaseProductName != null && databaseProductName.toLowerCase().contains("postgres")) {
            try {
                jdbcTemplate.execute("ALTER TABLE usuario ALTER COLUMN foto_url TYPE TEXT");
            } catch (RuntimeException exception) {
                logger.warn("No se pudo ajustar usuario.foto_url a TEXT. Se continuara con el esquema actual.", exception);
            }
        }

        if (cleanupDemoData) {
            cleanupKnownDemoData();
        }
    }

    private void cleanupKnownDemoData() {
        try {
            jdbcTemplate.update("""
                DELETE FROM equipo_miembro
                WHERE proyecto_id IN (
                    SELECT id FROM proyecto WHERE nombre IN ('Portal de Operaciones', 'App Comercial')
                )
                OR usuario_id IN (
                    SELECT id FROM usuario WHERE email IN ('admin@taskmaster.local', 'analista@taskmaster.local')
                )
                """);
            jdbcTemplate.update("""
                DELETE FROM tarea
                WHERE titulo IN ('Configurar backlog inicial', 'Levantar requerimientos del dashboard')
                OR proyecto_id IN (
                    SELECT id FROM proyecto WHERE nombre IN ('Portal de Operaciones', 'App Comercial')
                )
                OR usuario_asignado_id IN (
                    SELECT id FROM usuario WHERE email IN ('admin@taskmaster.local', 'analista@taskmaster.local')
                )
                """);
            jdbcTemplate.update("DELETE FROM proyecto WHERE nombre IN ('Portal de Operaciones', 'App Comercial')");
            jdbcTemplate.update("DELETE FROM usuario WHERE email IN ('admin@taskmaster.local', 'analista@taskmaster.local')");
        } catch (RuntimeException exception) {
            logger.warn("No se pudo limpiar la data demo conocida. Se continuara con los datos actuales.", exception);
        }
    }
}
