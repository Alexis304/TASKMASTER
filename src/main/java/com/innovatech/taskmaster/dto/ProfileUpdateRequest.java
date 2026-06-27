package com.innovatech.taskmaster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @NotBlank @Size(min = 3, max = 120) String nombres,
    @Size(max = 500) String fotoUrl
) {
}
