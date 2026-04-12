package com.bekri.services;

import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.request.UpdateMeRequest;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.User;
import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurStatut;
import com.bekri.repositories.UtilisateurRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour {@link UtilisateurService}.
 * Style workshop : connexion réelle à la base de données bekri_db.
 *
 * ⚠️ MySQL doit être démarré et bekri_db doit exister avant de lancer ces tests.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UtilisateurServiceIntegrationTest {

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // Email unique pour éviter les conflits avec les données existantes
    private static final String TEST_EMAIL = "test.workshop." + System.currentTimeMillis() + "@bekri.com";

    // ID de l'utilisateur créé — partagé entre les tests grâce à @Order
    private static Integer idUtilisateurTest;

    // ══════════════════════════════════════════════════════════════════════════
    // Nettoyage après tous les tests (comme @AfterAll dans le workshop)
    // ══════════════════════════════════════════════════════════════════════════

    @AfterAll
    static void cleanUp(@Autowired UtilisateurRepository repo) {
        // Supprime l'utilisateur de test créé pendant les tests
        if (idUtilisateurTest != null) {
            repo.findById(idUtilisateurTest).ifPresent(repo::delete);
            System.out.println("✅ Nettoyage : utilisateur test supprimé (id=" + idUtilisateurTest + ")");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 1 : Inscription (register)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Test 1 : register → utilisateur créé en base de données")
    void testRegister() {
        // Arrange
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("TestNom");
        dto.setPrenom("TestPrenom");
        dto.setEmail(TEST_EMAIL);
        dto.setMotDePasse("motDePasse123");
        dto.setTelephone("0612345678");
        dto.setDateNaissance(LocalDate.of(1995, 5, 15));

        // Act
        UtilisateurResponseDTO result = utilisateurService.register(dto);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("TestNom", result.getNom());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(UtilisateurStatut.ACTIF, result.getStatut());

        // Sauvegarder l'ID pour les tests suivants
        idUtilisateurTest = result.getId();
        System.out.println("✅ Utilisateur créé avec id=" + idUtilisateurTest);

        // Vérification directe en base
        assertTrue(utilisateurRepository.existsByEmail(TEST_EMAIL));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 2 : Récupérer par ID
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("Test 2 : getUtilisateurById → retourne le bon utilisateur depuis la DB")
    void testGetUtilisateurById() {
        // Arrange : l'utilisateur a été créé au test 1
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        // Act
        UtilisateurResponseDTO result = utilisateurService.getUtilisateurById(idUtilisateurTest);

        // Assert
        assertNotNull(result);
        assertEquals(idUtilisateurTest, result.getId());
        assertEquals("TestNom", result.getNom());
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 3 : Recherche (search)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Test 3 : search → l'utilisateur test apparaît dans les résultats")
    void testSearch() {
        // Act
        List<UtilisateurResponseDTO> results = utilisateurService.search("TestNom", null, null);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(
                results.stream().anyMatch(u -> u.getEmail().equals(TEST_EMAIL)),
                "L'utilisateur test doit apparaître dans les résultats de recherche"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 4 : Modifier (updateMe)
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("Test 4 : updateMe → nom et prénom modifiés en base de données")
    void testUpdateMe() {
        // Arrange
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        // Récupérer l'entité pour simuler le principal
        Utilisateur principal = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();

        UpdateMeRequest dto = new UpdateMeRequest();
        dto.setNom("NomModifie");
        dto.setPrenom("PrenomModifie");
        dto.setEmail(TEST_EMAIL); // même email

        // Act
        UtilisateurResponseDTO result = utilisateurService.updateMe(principal, dto);

        // Assert
        assertEquals("NomModifie", result.getNom());
        assertEquals("PrenomModifie", result.getPrenom());

        // Vérification directe en base
        Utilisateur enBase = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();
        assertEquals("NomModifie", enBase.getNom());
        assertEquals("PrenomModifie", enBase.getPrenom());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 5 : Modifier le statut
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("Test 5 : updateStatut → statut BLOQUE enregistré en base de données")
    void testUpdateStatut() {
        // Arrange
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        // Act
        UtilisateurResponseDTO result = utilisateurService.updateStatut(idUtilisateurTest, "bloque");

        // Assert
        assertEquals(UtilisateurStatut.BLOQUE, result.getStatut());

        // Vérification directe en base
        Utilisateur enBase = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();
        assertEquals(UtilisateurStatut.BLOQUE, enBase.getStatut());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 6 : Remettre actif
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("Test 6 : updateStatut → statut ACTIF restauré en base de données")
    void testReactiverUtilisateur() {
        // Arrange
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        // Act
        UtilisateurResponseDTO result = utilisateurService.updateStatut(idUtilisateurTest, "actif");

        // Assert
        assertEquals(UtilisateurStatut.ACTIF, result.getStatut());

        // deactivatedAt doit être remis à null
        Utilisateur enBase = utilisateurRepository.findById(idUtilisateurTest).orElseThrow();
        assertNull(enBase.getDeactivatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Test 7 : register email déjà utilisé
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(7)
    @DisplayName("Test 7 : register email déjà utilisé → exception levée")
    void testRegisterEmailDuplique() {
        // Arrange : TEST_EMAIL est déjà en base depuis le test 1
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("Autre");
        dto.setPrenom("Personne");
        dto.setEmail(TEST_EMAIL); // même email
        dto.setMotDePasse("pass");

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> utilisateurService.register(dto)
        );
    }
}