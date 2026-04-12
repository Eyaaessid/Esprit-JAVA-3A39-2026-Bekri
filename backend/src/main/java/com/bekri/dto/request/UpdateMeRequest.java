package com.bekri.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateMeRequest {

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    @Email
    private String email;

    private String telephone;

    @NotNull
    private LocalDate dateNaissance;

    private String avatar;

    /** Optionnel : renseigné uniquement pour changer le mot de passe. */
    private String motDePasse;
}

