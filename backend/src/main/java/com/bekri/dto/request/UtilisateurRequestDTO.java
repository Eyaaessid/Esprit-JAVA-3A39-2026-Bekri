package com.bekri.dto.request;

import com.bekri.enums.UtilisateurRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurRequestDTO {

    public interface OnCreate {}

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotBlank
    @Email
    private String email;

    @NotBlank(groups = OnCreate.class)
    private String motDePasse;

    private String telephone;

    @NotNull
    private LocalDate dateNaissance;

    @NotNull
    private UtilisateurRole role;
}
