package com.bekri.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatutRequest {
    /** Valeurs: actif|bloque|inactif|supprime */
    @NotBlank
    private String statut;
}

