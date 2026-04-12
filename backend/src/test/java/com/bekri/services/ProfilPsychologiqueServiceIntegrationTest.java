package com.bekri.services;

import com.bekri.dto.request.ProfilPsychologiqueRequestDTO;
import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.response.ProfilPsychologiqueResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.repositories.ProfilPsychologiqueRepository;
import com.bekri.repositories.UtilisateurRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProfilPsychologiqueRepository profilPsychologiqueRepository;

    private static final String TEST_EMAIL = "test.profil." + System.currentTimeMillis() + "@bekri.com";
    private static Integer idUtilisateurTest;

    // ══════════════════════════════════════════════════════════════════════════
    // Setup : créer un utilisateur de test
    // ══════════════════════════════════════════════════════════════════════════

    @BeforeAll
    static void setup(@Autowired UtilisateurService service) {
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("ProfilTest");
        dto.setPrenom("User");
        dto.setEmail(TEST_EMAIL);
        dto.setMotDePasse("pass123");
        idUtilisateurTest = service.register(dto).getId();
        System.out.println("✅ Utilisateur test créé avec id=" + idUtilisateurTest);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Nettoyage après tous les tests
    // ══════════════════════════════════════════════════════════════════════════

    @AfterAll
    static void cleanUp(
            @Autowired ProfilPsychologiqueRepository profilRepo,
            @Autowired UtilisateurRepository userRepo) {
        if (idUtilisateurTest != null) {
            profilRepo.findByUtilisateurId(idUtilisateurTest)
                    .ifPresent(profilRepo::delete);
            userRepo.findById(idUtilisateurTest)
                    .ifPresent(userRepo::delete);
            System.out.println("✅ Nettoyage terminé");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 1 : Créer un profil (upsert — création)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Test 1 : upsert → profil créé en base de données")
    void testCreerProfil() {
        // Arrange
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();

        ProfilPsychologiqueRequestDTO dto = new ProfilPsychologiqueRequestDTO();
        dto.setScoreGlobal(75);
        dto.setProfilType("Analytique");
        dto.setDateEvaluation(LocalDateTime.now());
        dto.setAiFeedback("Bon profil analytique.");

        // Act
        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.upsertForCurrentUser(utilisateur, dto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(75, result.getScoreGlobal());
        assertEquals("Analytique", result.getProfilType());
        assertEquals(idUtilisateurTest, result.getUtilisateurId());

        // Vérification directe en base
        assertTrue(profilPsychologiqueRepository.findByUtilisateurId(idUtilisateurTest).isPresent());
        System.out.println("✅ Profil créé avec id=" + result.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 2 : Récupérer le profil
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("Test 2 : getForCurrentUser → profil récupéré depuis la base de données")
    void testGetProfil() {
        // Arrange
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();

        // Act
        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.getForCurrentUser(utilisateur);

        // Assert
        assertNotNull(result);
        assertEquals(75, result.getScoreGlobal());
        assertEquals("Analytique", result.getProfilType());
        assertEquals("Bon profil analytique.", result.getAiFeedback());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 3 : Modifier le profil (upsert — mise à jour)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Test 3 : upsert → profil mis à jour en base de données")
    void testModifierProfil() {
        // Arrange
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();

        ProfilPsychologiqueRequestDTO dto = new ProfilPsychologiqueRequestDTO();
        dto.setScoreGlobal(90);
        dto.setProfilType("Leader");
        dto.setDateEvaluation(LocalDateTime.now());
        dto.setAiFeedback("Profil leadership dominant.");

        // Act
        ProfilPsychologiqueResponseDTO result =
                profilPsychologiqueService.upsertForCurrentUser(utilisateur, dto);

        // Assert
        assertEquals(90, result.getScoreGlobal());
        assertEquals("Leader", result.getProfilType());

        // Vérification directe en base
        var enBase = profilPsychologiqueRepository.findByUtilisateurId(idUtilisateurTest).orElseThrow();
        assertEquals(90, enBase.getScoreGlobal());
        assertEquals("Leader", enBase.getProfilType());
    }
}