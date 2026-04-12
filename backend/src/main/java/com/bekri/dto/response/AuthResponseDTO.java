package com.bekri.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {

    private String message;
    private UtilisateurResponseDTO utilisateur;
    private String token;
}
