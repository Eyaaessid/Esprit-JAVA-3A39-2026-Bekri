package com.bekri.services;

import com.bekri.dto.request.ProfilPsychologiqueRequestDTO;
import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.response.ProfilPsychologiqueResponseDTO;
import com.bekri.entities.Utilisateur;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour {@link ProfilPsychologiqueService}.
 * Style workshop : connexion réelle à la base de données bekri_db.
 *
 * ⚠️ MySQL doit être démarré et bekri_db doit exister avant de lancer ces tests.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfilPsychologiqueServiceIntegrationTest {

    @Autowired
    private ProfilPsychologiqueService profilPsychologiqueService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    private static final String TEST_EMAIL = "test.profil." + System.currentTimeMillis() + "@bekri.com";
    private static Integer idUtilisateurTest;

    @BeforeAll
    static void setup(@Autowired UtilisateurService service) {
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("ProfilTest");
        dto.setPrenom("User");
        dto.setEmail(TEST_EMAIL);
        dto.setMotDePasse("pass123456");
        dto.setTelephone("0612345678");
        dto.setDateNaissance(LocalDate.of(1992, 3, 10));
        idUtilisateurTest = service.register(dto).getId();
        System.out.println("✅ Utilisateur test créé avec id=" + idUtilisateurTest);
    }

    @AfterAll
    static void cleanUp(@Autowired UtilisateurService utilisateurService) {
        if (idUtilisateurTest != null) {
            utilisateurService.hardDeleteUtilisateur(idUtilisateurTest);
            System.out.println("✅ Nettoyage terminé");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1 : upsert → profil créé en base de données")
    void testCreerProfil() {
        Utilisateur utilisateur = customUserDetailsService.loadUserById(idUtilisateurTest);

        ProfilPsychologiqueRequestDTO dto = new ProfilPsychologiqueRequestDTO();
        dto.setScoreGlobal(75);
        dto.setProfilType("Analytique");
        dto.setDateEvaluation(LocalDateTime.now());
        dto.setAiFeedback("Bon profil analytique.");

        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.upsertForCurrentUser(utilisateur, dto);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(75, result.getScoreGlobal());
        assertEquals("Analytique", result.getProfilType());
        assertEquals(idUtilisateurTest, result.getUtilisateurId());

        assertDoesNotThrow(() -> profilPsychologiqueService.getForCurrentUser(utilisateur));
        System.out.println("✅ Profil créé avec id=" + result.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Test 2 : getForCurrentUser → profil récupéré depuis la base de données")
    void testGetProfil() {
        Utilisateur utilisateur = customUserDetailsService.loadUserById(idUtilisateurTest);

        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.getForCurrentUser(utilisateur);

        assertNotNull(result);
        assertEquals(75, result.getScoreGlobal());
        assertEquals("Analytique", result.getProfilType());
        assertEquals("Bon profil analytique.", result.getAiFeedback());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3 : upsert → profil mis à jour en base de données")
    void testModifierProfil() {
        Utilisateur utilisateur = customUserDetailsService.loadUserById(idUtilisateurTest);

        ProfilPsychologiqueRequestDTO dto = new ProfilPsychologiqueRequestDTO();
        dto.setScoreGlobal(90);
        dto.setProfilType("Leader");
        dto.setDateEvaluation(LocalDateTime.now());
        dto.setAiFeedback("Profil leadership dominant.");

        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.upsertForCurrentUser(utilisateur, dto);

        assertEquals(90, result.getScoreGlobal());
        assertEquals("Leader", result.getProfilType());

        ProfilPsychologiqueResponseDTO enBase =
                profilPsychologiqueService.getForCurrentUser(utilisateur);
        assertEquals(90, enBase.getScoreGlobal());
        assertEquals("Leader", enBase.getProfilType());
    }
}
