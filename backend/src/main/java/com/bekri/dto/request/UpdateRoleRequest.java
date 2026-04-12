package com.bekri.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    /** Valeurs: user|coach|admin */
    @NotBlank
    private String role;
}

