package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.AuthRequest;
import com.innovatech.taskmaster.dto.AuthResponse;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse authenticate(AuthRequest request) {
        return usuarioRepository.findByEmail(request.email())
            .map(usuario -> buildResponse(usuario, request.password()))
            .orElse(new AuthResponse(false, "Credenciales invalidas"));
    }

    private AuthResponse buildResponse(Usuario usuario, String rawPassword) {
        boolean authenticated = passwordEncoder.matches(rawPassword, usuario.getPassword());
        String message = authenticated ? "Autenticacion correcta" : "Credenciales invalidas";
        return new AuthResponse(authenticated, message);
    }
}
