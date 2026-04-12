package com.bekri.controllers;

import com.bekri.config.OpenApiConfig;
import com.bekri.dto.request.ProfilPsychologiqueRequestDTO;
import com.bekri.dto.response.ProfilPsychologiqueResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.services.ProfilPsychologiqueService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profil-psychologique")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProfilPsychologiqueController {

    private final ProfilPsychologiqueService profilPsychologiqueService;

    @GetMapping
    public ResponseEntity<ProfilPsychologiqueResponseDTO> get(@AuthenticationPrincipal Utilisateur utilisateur) {
        return ResponseEntity.ok(profilPsychologiqueService.getForCurrentUser(utilisateur));
    }

    @PutMapping
    public ResponseEntity<ProfilPsychologiqueResponseDTO> put(
            @AuthenticationPrincipal Utilisateur utilisateur,
            @Valid @RequestBody ProfilPsychologiqueRequestDTO dto) {
        return ResponseEntity.ok(profilPsychologiqueService.upsertForCurrentUser(utilisateur, dto));
    }
}
