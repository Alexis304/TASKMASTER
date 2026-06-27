package com.innovatech.taskmaster.controller;

import com.innovatech.taskmaster.dto.AuthRequest;
import com.innovatech.taskmaster.dto.AuthProviderResponse;
import com.innovatech.taskmaster.dto.AuthResponse;
import com.innovatech.taskmaster.dto.CurrentUserResponse;
import com.innovatech.taskmaster.dto.ProfileUpdateRequest;
import com.innovatech.taskmaster.dto.RegisterRequest;
import com.innovatech.taskmaster.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody AuthRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpServletRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(authService.register(request, httpServletRequest));
    }

    @GetMapping("/providers")
    public ResponseEntity<AuthProviderResponse> providers() {
        return ResponseEntity.ok(authService.providers());
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication));
    }

    @PutMapping("/me")
    public ResponseEntity<CurrentUserResponse> updateProfile(
        @Valid @RequestBody ProfileUpdateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(authService.updateProfile(request, authentication));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
