package com.bekri.controllers;

import com.bekri.config.OpenApiConfig;
import com.bekri.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes admin dédiées (chemins distincts de {@code /api/utilisateurs}).
 */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminUtilisateurController {

    private final UtilisateurService utilisateurService;

    @Operation(summary = "Suppression définitive d'un utilisateur (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> hardDelete(@PathVariable Integer id) {
        utilisateurService.hardDeleteUtilisateur(id);
        return ResponseEntity.noContent().build();
    }
}
