package com.bekri.dto.response;

import com.bekri.enums.UtilisateurStatut;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurResponseDTO {

    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    /** Valeurs: "user" | "coach" | "admin" (aligné sur la colonne DB). */
    private String role;
    private UtilisateurStatut statut;
    private LocalDateTime createdAt;
    /** URL relative (ex. /uploads/avatars/xxx.webp) ou null. */
    private String avatar;
}
