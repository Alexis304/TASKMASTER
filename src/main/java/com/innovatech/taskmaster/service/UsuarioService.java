package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.RegisterRequest;
import com.innovatech.taskmaster.dto.UsuarioCreateRequest;
import com.innovatech.taskmaster.dto.UsuarioResponse;
import com.innovatech.taskmaster.dto.UsuarioUpdateRequest;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.TareaRepository;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import com.innovatech.taskmaster.soap.client.DniPersona;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final PasswordEncoder passwordEncoder;
    private final DniRestClient dniRestClient;

    public UsuarioService(
        UsuarioRepository usuarioRepository,
        TareaRepository tareaRepository,
        PasswordEncoder passwordEncoder,
        DniRestClient dniRestClient
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tareaRepository = tareaRepository;
        this.passwordEncoder = passwordEncoder;
        this.dniRestClient = dniRestClient;
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll(Sort.by("nombres"))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public UsuarioResponse crear(UsuarioCreateRequest request) {
        Usuario usuario = crearUsuario(request.dni(), request.nombres(), request.email(), request.password());
        return toResponse(usuario);
    }

    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String normalizedEmail = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con este correo.");
        }

        String normalizedDni = normalizeDni(request.dni());
        DniPersona persona = buscarPersonaPorDni(normalizedDni);
        String nombreCompleto = resolveNombreCompleto(persona, request.nombres());

        usuario.setEmail(normalizedEmail);
        usuario.setDni(normalizedDni);
        usuario.setNombres(nombreCompleto);

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    public void eliminar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        if (tareaRepository.existsByUsuarioAsignadoId(id)) {
            throw new IllegalArgumentException("No se puede eliminar un usuario con tareas asignadas.");
        }

        usuarioRepository.deleteById(id);
    }

    public Usuario registrar(RegisterRequest request) {
        return crearUsuario(request.dni(), request.nombres(), request.email(), request.password());
    }

    public Usuario crearUsuario(String dni, String nombres, String email, String password) {
        String normalizedDni = normalizeDni(dni);
        String normalizedNombre = normalizeNombre(nombres);
        String normalizedEmail = normalizeEmail(email);
        if (usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con este correo.");
        }

        DniPersona persona = buscarPersonaPorDni(normalizedDni);
        String nombreCompleto = resolveNombreCompleto(persona, normalizedNombre);

        Usuario usuario = new Usuario();
        usuario.setEmail(normalizedEmail);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setDni(normalizedDni);
        usuario.setNombres(nombreCompleto);

        return usuarioRepository.save(usuario);
    }

    private DniPersona buscarPersonaPorDni(String dni) {
        try {
            DniPersona persona = dniRestClient.obtenerPersonaPorDni(dni);
            validarPersonaActiva(persona);
            return persona;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String resolveNombreCompleto(DniPersona persona, String fallbackNombre) {
        if (persona != null && persona.nombreCompleto() != null && !persona.nombreCompleto().isBlank()) {
            return normalizeNombre(persona.nombreCompleto());
        }
        return normalizeNombre(fallbackNombre);
    }

    private void validarPersonaActiva(DniPersona persona) {
        if (!"ACTIVO".equalsIgnoreCase(persona.estado())) {
            throw new IllegalArgumentException("El DNI no esta habilitado para registrarse.");
        }
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getDni(), usuario.getNombres());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeNombre(String nombres) {
        return nombres.trim().replaceAll("\\s+", " ");
    }

    private String normalizeDni(String dni) {
        return dni == null ? "" : dni.replaceAll("\\D", "");
    }
}
