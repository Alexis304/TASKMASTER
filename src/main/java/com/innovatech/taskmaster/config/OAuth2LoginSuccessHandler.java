package com.innovatech.taskmaster.config;

import com.innovatech.taskmaster.model.Usuario;
import com.innovatech.taskmaster.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2LoginSuccessHandler(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(Usuario::new);
                usuario.setEmail(email);
                usuario.setNombres(resolveDisplayName(oauth2User));

                if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                    usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                }

                usuarioRepository.save(usuario);
            }
        }

        response.sendRedirect("/?google=success");
    }

    private String resolveDisplayName(OAuth2User oauth2User) {
        String name = oauth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String givenName = oauth2User.getAttribute("given_name");
        return givenName != null && !givenName.isBlank() ? givenName : "Usuario Google";
    }
}
