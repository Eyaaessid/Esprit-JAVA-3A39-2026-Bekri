package com.bekri.controllers;

import com.bekri.config.OpenApiConfig;
import com.bekri.dto.request.UpdateMeRequest;
import com.bekri.dto.request.UpdateRoleRequest;
import com.bekri.dto.request.UpdateStatutRequest;
import com.bekri.dto.request.UtilisateurRequestDTO;
import com.bekri.dto.request.UtilisateurRequestDTO.OnCreate;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Liste / filtres alignés dashboard admin Symfony. JWT : voir {@link com.bekri.controllers.AuthController}.
 */
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @Operation(
            summary = "Lister les utilisateurs (filtres)",
            description = "ADMIN uniquement. Recherche LIKE sur nom, prénom, email ; filtres optionnels role et statut (valeurs DB en minuscules).")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UtilisateurResponseDTO>> list(
            @Parameter(description = "Recherche (contient) sur nom, prénom, email", example = "jean")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtre rôle", example = "coach")
            @RequestParam(required = false) String role,
            @Parameter(description = "Filtre statut DB", example = "actif")
            @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(utilisateurService.search(search, role, statut));
    }

    @Operation(summary = "Profil utilisateur connecté")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponseDTO> me(@AuthenticationPrincipal Utilisateur principal) {
        return ResponseEntity.ok(utilisateurService.me(principal));
    }

    @Operation(
            summary = "Mettre à jour son profil",
            description = "Champs personnels uniquement (pas de role ni statut).")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UtilisateurResponseDTO> updateMe(
            @AuthenticationPrincipal Utilisateur principal,
            @Valid @RequestBody UpdateMeRequest dto) {
        return ResponseEntity.ok(utilisateurService.updateMe(principal, dto));
    }

    @Operation(summary = "Téléverser sa photo de profil (image ≤ 2 Mo)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UtilisateurResponseDTO> uploadMyAvatar(
            @AuthenticationPrincipal Utilisateur principal,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(utilisateurService.uploadMyAvatar(principal, file));
    }

    @Operation(summary = "Détail utilisateur")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurService.isSelf(#id)")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UtilisateurResponseDTO> getUtilisateurById(
            @Parameter(description = "ID utilisateur", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(utilisateurService.getUtilisateurById(id));
    }

    @Operation(summary = "Créer un utilisateur (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UtilisateurResponseDTO> createUtilisateur(
            @Validated({Default.class, OnCreate.class}) @RequestBody UtilisateurRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.createUtilisateur(dto));
    }

    @Operation(summary = "Mettre à jour un utilisateur (ADMIN ou soi-même)")
    @PreAuthorize("hasRole('ADMIN') or @utilisateurService.isSelf(#id)")
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<UtilisateurResponseDTO> updateUtilisateur(
            @PathVariable Integer id, @Valid @RequestBody UtilisateurRequestDTO dto) {
        return ResponseEntity.ok(utilisateurService.updateUtilisateur(id, dto));
    }

    @Operation(
            summary = "Modifier uniquement le rôle (ADMIN)",
            description = "Transition de type JPA (User/Coach/Admin), sans UPDATE SQL natif sur la colonne discriminator.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id:\\d+}/role")
    public ResponseEntity<UtilisateurResponseDTO> patchRole(
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nouveau rôle",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateRoleRequest.class),
                            examples = @ExampleObject(value = "{\"role\": \"coach\"}")))
            @Valid @RequestBody UpdateRoleRequest dto) {
        return ResponseEntity.ok(utilisateurService.updateRole(id, dto.getRole()));
    }

    @Operation(
            summary = "Modifier uniquement le statut (ADMIN)",
            description = "Persiste les chaînes minuscules Symfony (actif, bloque, …). Si supprime : deactivatedAt (+ deactivatedBy).")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id:\\d+}/statut")
    public ResponseEntity<UtilisateurResponseDTO> patchStatut(
            @PathVariable Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateStatutRequest.class),
                            examples = @ExampleObject(value = "{\"statut\": \"bloque\"}")))
            @Valid @RequestBody UpdateStatutRequest dto) {
        return ResponseEntity.ok(utilisateurService.updateStatut(id, dto.getStatut()));
    }

    @Operation(
            summary = "Suppression logique (ADMIN)",
            description = "statut=supprime, deactivatedAt renseigné (pas de DELETE SQL).")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Integer id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.noContent().build();
    }
}
