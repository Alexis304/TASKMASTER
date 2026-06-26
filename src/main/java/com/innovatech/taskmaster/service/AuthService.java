package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.AuthRequest;
import com.innovatech.taskmaster.dto.AuthResponse;
import com.innovatech.taskmaster.dto.CurrentUserResponse;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;

    public AuthService(UsuarioRepository usuarioRepository, AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(AuthRequest request, HttpServletRequest httpServletRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Usuario usuario = usuarioRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return new AuthResponse(true, "Autenticacion correcta", usuario.getEmail(), usuario.getNombres());
    }

    public CurrentUserResponse currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("No hay una sesion activa");
        }

        Usuario usuario = usuarioRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return new CurrentUserResponse(usuario.getId(), usuario.getEmail(), usuario.getNombres());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }
}
