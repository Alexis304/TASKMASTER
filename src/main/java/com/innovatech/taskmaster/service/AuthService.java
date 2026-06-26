package com.innovatech.taskmaster.service;

import com.innovatech.taskmaster.dto.AuthRequest;
import com.innovatech.taskmaster.dto.AuthProviderResponse;
import com.innovatech.taskmaster.dto.AuthResponse;
import com.innovatech.taskmaster.dto.CurrentUserResponse;
import com.innovatech.taskmaster.dto.RegisterRequest;
import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final String googleClientId;

    public AuthService(
        UsuarioRepository usuarioRepository,
        UsuarioService usuarioService,
        AuthenticationManager authenticationManager,
        @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId
    ) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.googleClientId = googleClientId;
    }

    public AuthResponse login(AuthRequest request, HttpServletRequest httpServletRequest) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return new AuthResponse(true, "Autenticacion correcta", usuario.getEmail(), usuario.getNombres());
    }

    public AuthResponse register(RegisterRequest request, HttpServletRequest httpServletRequest) {
        Usuario usuario = usuarioService.registrar(request);
        return login(new AuthRequest(usuario.getEmail(), request.password()), httpServletRequest);
    }

    public CurrentUserResponse currentUser(Authentication authentication) {
        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken
        ) {
            throw new IllegalArgumentException("No hay una sesion activa");
        }

        Usuario usuario = usuarioRepository.findByEmail(resolveAuthenticatedEmail(authentication))
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        return new CurrentUserResponse(usuario.getId(), usuario.getEmail(), usuario.getNombres());
    }

    public AuthProviderResponse providers() {
        boolean googleEnabled = googleClientId != null && !googleClientId.isBlank();
        return new AuthProviderResponse(googleEnabled, "/oauth2/authorization/google");
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername().trim().toLowerCase();
        }

        return authentication.getName().trim().toLowerCase();
    }
}
