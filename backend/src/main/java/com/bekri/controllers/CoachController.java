package com.bekri.controllers;

import com.bekri.config.OpenApiConfig;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints réservés aux Coachs (ROLE_COACH).
 *
 * Démontre l'héritage de rôle en session :
 *  - User  → ROLE_USER seulement         → 403 sur tous ces endpoints
 *  - Coach → ROLE_USER + ROLE_COACH      → 200 sur tous ces endpoints
 *  - Admin → ROLE_USER + ROLE_ADMIN      → 403 sur tous ces endpoints
 *
 * Le JWT du coach contient : { "role": "coach" } → héritage visible en session.
 */
@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CoachController {

    private final UtilisateurService utilisateurService;

    /**
     * Tableau de bord coach.
     * User → 403 | Coach → 200 | Admin → 403
     */
    @Operation(
            summary = "Tableau de bord Coach",
            description = "Réservé aux coachs uniquement. " +
                    "Démontre l'héritage : Coach hérite des droits User ET a des accès supplémentaires. " +
                    "Le JWT contient role=coach, attribué à la connexion (héritage en session).")
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @AuthenticationPrincipal Utilisateur principal) {

        UtilisateurResponseDTO profil = utilisateurService.me(principal);

        return ResponseEntity.ok(Map.of(
                "message", "Bienvenue sur votre tableau de bord Coach",
                "profil", profil,
                "role", "coach",
                "acces_supplementaires", List.of(
                        "Organiser des événements",
                        "Voir la liste de tous les utilisateurs",
                        "Inviter des utilisateurs à ses événements"
                )
        ));
    }

    /**
     * Liste tous les utilisateurs (role=user) — pour organiser des événements.
     * User → 403 | Coach → 200 | Admin → 403
     */
    @Operation(
            summary = "Liste des utilisateurs (Coach)",
            description = "Un coach peut voir tous les utilisateurs pour les inviter à ses événements. " +
                    "Un simple User n'a pas cet accès.")
    @PreAuthorize("hasRole('COACH')")
    @GetMapping("/utilisateurs")
    public ResponseEntity<List<UtilisateurResponseDTO>> listeUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.search(null, "user", null));
    }
}