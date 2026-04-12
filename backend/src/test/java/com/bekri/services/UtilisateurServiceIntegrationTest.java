package com.bekri.services;

import com.bekri.dto.request.RegisterDTO;
import com.bekri.dto.request.UpdateMeRequest;
import com.bekri.dto.response.UtilisateurResponseDTO;
import com.bekri.entities.Utilisateur;
import com.bekri.enums.UtilisateurStatut;
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
    private CustomUserDetailsService customUserDetailsService;

    // Email unique pour éviter les conflits avec les données existantes
    private static final String TEST_EMAIL = "test.workshop." + System.currentTimeMillis() + "@bekri.com";

    // ID de l'utilisateur créé — partagé entre les tests grâce à @Order
    private static Integer idUtilisateurTest;

    @AfterAll
    static void cleanUp(@Autowired UtilisateurService service) {
        if (idUtilisateurTest != null) {
            service.hardDeleteUtilisateur(idUtilisateurTest);
            System.out.println("✅ Nettoyage : utilisateur test supprimé (id=" + idUtilisateurTest + ")");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1 : register → utilisateur créé en base de données")
    void testRegister() {
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("TestNom");
        dto.setPrenom("TestPrenom");
        dto.setEmail(TEST_EMAIL);
        dto.setMotDePasse("motDePasse123");
        dto.setTelephone("0612345678");
        dto.setDateNaissance(LocalDate.of(1995, 5, 15));

        UtilisateurResponseDTO result = utilisateurService.register(dto);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("TestNom", result.getNom());
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(UtilisateurStatut.ACTIF, result.getStatut());

        idUtilisateurTest = result.getId();
        System.out.println("✅ Utilisateur créé avec id=" + idUtilisateurTest);

        assertDoesNotThrow(() -> customUserDetailsService.loadUserByUsername(TEST_EMAIL));
    }

    @Test
    @Order(2)
    @DisplayName("Test 2 : getUtilisateurById → retourne le bon utilisateur depuis la DB")
    void testGetUtilisateurById() {
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        UtilisateurResponseDTO result = utilisateurService.getUtilisateurById(idUtilisateurTest);

        assertNotNull(result);
        assertEquals(idUtilisateurTest, result.getId());
        assertEquals("TestNom", result.getNom());
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    @Order(3)
    @DisplayName("Test 3 : search → l'utilisateur test apparaît dans les résultats")
    void testSearch() {
        List<UtilisateurResponseDTO> results = utilisateurService.search("TestNom", null, null);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(
                results.stream().anyMatch(u -> u.getEmail().equals(TEST_EMAIL)),
                "L'utilisateur test doit apparaître dans les résultats de recherche"
        );
    }

    @Test
    @Order(4)
    @DisplayName("Test 4 : updateMe → nom et prénom modifiés en base de données")
    void testUpdateMe() {
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        Utilisateur principal = customUserDetailsService.loadUserById(idUtilisateurTest);

        UpdateMeRequest dto = new UpdateMeRequest();
        dto.setNom("NomModifie");
        dto.setPrenom("PrenomModifie");
        dto.setEmail(TEST_EMAIL);
        dto.setDateNaissance(LocalDate.of(1995, 5, 15));

        UtilisateurResponseDTO result = utilisateurService.updateMe(principal, dto);

        assertEquals("NomModifie", result.getNom());
        assertEquals("PrenomModifie", result.getPrenom());

        Utilisateur enBase = customUserDetailsService.loadUserById(idUtilisateurTest);
        assertEquals("NomModifie", enBase.getNom());
        assertEquals("PrenomModifie", enBase.getPrenom());
    }

    @Test
    @Order(5)
    @DisplayName("Test 5 : updateStatut → statut BLOQUE enregistré en base de données")
    void testUpdateStatut() {
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        UtilisateurResponseDTO result = utilisateurService.updateStatut(idUtilisateurTest, "bloque");

        assertEquals(UtilisateurStatut.BLOQUE, result.getStatut());

        Utilisateur enBase = customUserDetailsService.loadUserById(idUtilisateurTest);
        assertEquals(UtilisateurStatut.BLOQUE, enBase.getStatut());
    }

    @Test
    @Order(6)
    @DisplayName("Test 6 : updateStatut → statut ACTIF restauré en base de données")
    void testReactiverUtilisateur() {
        assertNotNull(idUtilisateurTest, "Le test 1 doit s'exécuter avant");

        UtilisateurResponseDTO result = utilisateurService.updateStatut(idUtilisateurTest, "actif");

        assertEquals(UtilisateurStatut.ACTIF, result.getStatut());

        Utilisateur enBase = customUserDetailsService.loadUserById(idUtilisateurTest);
        assertNull(enBase.getDeactivatedAt());
    }

    @Test
    @Order(7)
    @DisplayName("Test 7 : register email déjà utilisé → exception levée")
    void testRegisterEmailDuplique() {
        RegisterDTO dto = new RegisterDTO();
        dto.setNom("Autre");
        dto.setPrenom("Personne");
        dto.setEmail(TEST_EMAIL);
        dto.setMotDePasse("pass123456");
        dto.setDateNaissance(LocalDate.of(1990, 1, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> utilisateurService.register(dto)
        );
    }
}
