package com.innovatech.taskmaster.config;

import com.innovatech.taskmaster.model.Proyecto;
import com.innovatech.taskmaster.model.Tarea;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.ProyectoRepository;
import com.innovatech.taskmaster.repository.TareaRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataLoader {

    @Bean
    public CommandLineRunner loadDemoData(
        UsuarioRepository usuarioRepository,
        ProyectoRepository proyectoRepository,
        TareaRepository tareaRepository,
        PasswordEncoder passwordEncoder,
        @Value("${taskmaster.demo.admin.email}") String adminEmail,
        @Value("${taskmaster.demo.admin.password}") String adminPassword
    ) {
        return args -> {
            if (usuarioRepository.count() > 0 || proyectoRepository.count() > 0) {
                return;
            }

            Usuario admin = new Usuario();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setDni("70112233");
            admin.setNombres("Alexis Ramirez");
            usuarioRepository.save(admin);

            Usuario analista = new Usuario();
            analista.setEmail("analista@taskmaster.local");
            analista.setPassword(passwordEncoder.encode("Analista123*"));
            analista.setDni("70889911");
            analista.setNombres("Camila Torres");
            usuarioRepository.save(analista);

            Proyecto portal = new Proyecto();
            portal.setNombre("Portal de Operaciones");
            portal.setDescripcion("Mejoras internas para el seguimiento corporativo.");
            proyectoRepository.save(portal);

            Proyecto mobile = new Proyecto();
            mobile.setNombre("App Comercial");
            mobile.setDescripcion("Backoffice de soporte para el canal de ventas.");
            proyectoRepository.save(mobile);

            Tarea tarea1 = new Tarea();
            tarea1.setTitulo("Configurar backlog inicial");
            tarea1.setDescripcion("Definir alcance, responsables y prioridad.");
            tarea1.setFechaLimite(LocalDate.now().plusDays(2));
            tarea1.setProyecto(portal);
            tarea1.setUsuarioAsignado(admin);
            tareaRepository.save(tarea1);

            Tarea tarea2 = new Tarea();
            tarea2.setTitulo("Levantar requerimientos del dashboard");
            tarea2.setDescripcion("Recopilar informacion con el equipo comercial.");
            tarea2.setFechaLimite(LocalDate.now().plusDays(4));
            tarea2.setProyecto(mobile);
            tarea2.setUsuarioAsignado(analista);
            tareaRepository.save(tarea2);
        };
    }
}
