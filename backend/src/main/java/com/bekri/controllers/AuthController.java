package com.bekri.controllers;

import com.bekri.dto.request.LoginDTO;
import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.response.AuthResponseDTO;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.config.OpenApiConfig;
import com.bekri.services.CustomUserDetailsService;
import com.bekri.services.JwtService;
import com.bekri.services.UtilisateurService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Postman (JWT) :
 * <ol>
 *   <li>POST /api/auth/register → 201, récupérer {@code token} dans la réponse JSON</li>
 *   <li>GET /api/auth/me avec {@code Authorization: Bearer &lt;token&gt;} → 200 + utilisateur</li>
 *   <li>GET /api/utilisateurs avec token rôle {@code user} → 403</li>
 *   <li>Avec un compte {@code admin} en DB : GET /api/utilisateurs + Bearer → 200</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurService utilisateurService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterDTO dto) {
        UtilisateurResponseDTO utilisateurDto = utilisateurService.register(dto);
        Utilisateur entity = (Utilisateur) customUserDetailsService.loadUserByUsername(dto.getEmail());
        String token = jwtService.generateToken(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDTO("Inscription réussie", utilisateurDto, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        UtilisateurResponseDTO utilisateurDto = utilisateurService.login(dto);
        Utilisateur entity = (Utilisateur) customUserDetailsService.loadUserByUsername(dto.getEmail());
        String token = jwtService.generateToken(entity);
        return ResponseEntity.ok(new AuthResponseDTO("Connexion réussie", utilisateurDto, token));
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponseDTO> me(@AuthenticationPrincipal Utilisateur principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(utilisateurService.toResponseDTO(principal));
    }
}
