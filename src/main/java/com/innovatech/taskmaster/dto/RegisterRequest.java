package com.innovatech.taskmaster.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Pattern(regexp = "^(?=(?:.*\\d){8}\\D*$)[\\d\\s.-]+$", message = "debe contener exactamente 8 digitos") String dni,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 72) String password
) {
}
