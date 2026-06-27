package com.innovatech.taskmaster.config;

import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminAccountInitializer {

    @Bean
    public CommandLineRunner createOptionalAdminAccount(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        @Value("${taskmaster.seed.admin.enabled:false}") boolean enabled,
        @Value("${taskmaster.demo.admin.email}") String adminEmail,
        @Value("${taskmaster.demo.admin.password}") String adminPassword
    ) {
        return args -> {
            if (!enabled) {
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (usuarioRepository.existsByEmail(normalizedEmail)) {
                return;
            }

            Usuario admin = new Usuario();
            admin.setEmail(normalizedEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setDni("00000000");
            admin.setNombres("Administrador TaskMaster");
            usuarioRepository.save(admin);
        };
    }
}
