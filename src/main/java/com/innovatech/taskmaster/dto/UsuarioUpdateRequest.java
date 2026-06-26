package com.innovatech.taskmaster.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
    @NotBlank @Pattern(regexp = "\\d{8}", message = "debe tener 8 digitos") String dni,
    @NotBlank @Email String email,
    @Size(min = 8, max = 72) String password
) {
}
