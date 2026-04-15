package tn.esprit.pijavafx.service;

import org.junit.jupiter.api.*;
import tn.esprit.pijavafx.model.ObjectifBienEtreDto;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ObjectifServiceHttp
 * Suit le pattern du workshop : @BeforeAll, @Order, @AfterEach
 * Base de données MySQL réelle requise (Spring Boot backend sur http://127.0.0.1:8080)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ObjectifServiceTest {

    static IObjectifService service;
    static int idObjectifTest = -1;

    // ── Setup ─────────────────────────────────────────────────────────────────
    @BeforeAll
    static void setup() {
        service = new ObjectifServiceJdbc();
    }

    // ── Nettoyage après chaque test ───────────────────────────────────────────
    @AfterEach
    void cleanUp() {
        // Si un ID de test a été créé, on le supprime pour garder la BD propre
        if (idObjectifTest != -1) {
            try {
                service.delete(idObjectifTest);
            } catch (Exception ignored) {
                // Déjà supprimé par le test lui-même (ex : testSupprimer)
            }
            idObjectifTest = -1;
        }
    }

    // ── Test 1 : Ajouter un objectif ──────────────────────────────────────────
    @Test
    @Order(1)
    void testAjouterObjectif() throws Exception {
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setTitre("TestObjectif_Unitaire");
        dto.setType("humeur");
        dto.setStatut("en_cours");
        dto.setDescription("Créé par test unitaire");
        dto.setValeurCible(100.0);
        dto.setValeurActuelle(0.0);
        dto.setDateDebut(LocalDate.of(2025, 1, 1));
        dto.setDateFin(LocalDate.of(2025, 12, 31));
        dto.setUtilisateurId(1);

        service.create(dto);

        List<ObjectifBienEtreDto> objectifs = service.getAll();
        assertNotNull(objectifs, "La liste ne doit pas être null");
        assertFalse(objectifs.isEmpty(), "La liste ne doit pas être vide après ajout");

        boolean trouve = objectifs.stream()
                .anyMatch(o -> "TestObjectif_Unitaire".equals(o.getTitre()));
        assertTrue(trouve, "L'objectif ajouté doit être retrouvé dans la liste");

        // Récupérer l'ID pour les tests suivants et le nettoyage
        idObjectifTest = objectifs.stream()
                .filter(o -> "TestObjectif_Unitaire".equals(o.getTitre()))
                .mapToInt(ObjectifBienEtreDto::getId)
                .max()
                .orElse(-1);
    }

    // ── Test 2 : Afficher tous les objectifs ──────────────────────────────────
    @Test
    @Order(2)
    void testAfficherObjectifs() throws Exception {
        // Créer d'abord un objectif pour s'assurer que la liste n'est pas vide
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setTitre("TestObjectif_Afficher");
        dto.setType("sommeil");
        dto.setStatut("en_cours");
        dto.setValeurCible(50.0);
        dto.setValeurActuelle(10.0);
        dto.setDateDebut(LocalDate.of(2025, 1, 1));
        dto.setDateFin(LocalDate.of(2025, 6, 30));
        dto.setUtilisateurId(1);
        service.create(dto);

        List<ObjectifBienEtreDto> objectifs = service.getAll();

        assertNotNull(objectifs, "getAll() ne doit pas retourner null");
        assertFalse(objectifs.isEmpty(), "La liste doit contenir au moins un objectif");

        // Vérifier que les objets retournés ont bien un titre non null
        objectifs.forEach(o ->
                assertNotNull(o.getTitre(), "Chaque objectif doit avoir un titre")
        );

        // Récupérer l'ID du test pour le nettoyage
        idObjectifTest = objectifs.stream()
                .filter(o -> "TestObjectif_Afficher".equals(o.getTitre()))
                .mapToInt(ObjectifBienEtreDto::getId)
                .max()
                .orElse(-1);
    }

    // ── Test 3 : Modifier un objectif ─────────────────────────────────────────
    @Test
    @Order(3)
    void testModifierObjectif() throws Exception {
        // Créer un objectif à modifier
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setTitre("TestObjectif_AvantModif");
        dto.setType("poids");
        dto.setStatut("en_cours");
        dto.setValeurCible(80.0);
        dto.setValeurActuelle(90.0);
        dto.setDateDebut(LocalDate.of(2025, 1, 1));
        dto.setDateFin(LocalDate.of(2025, 12, 31));
        dto.setUtilisateurId(1);
        service.create(dto);

        // Récupérer l'ID créé
        List<ObjectifBienEtreDto> avant = service.getAll();
        idObjectifTest = avant.stream()
                .filter(o -> "TestObjectif_AvantModif".equals(o.getTitre()))
                .mapToInt(ObjectifBienEtreDto::getId)
                .max()
                .orElse(-1);

        assertNotEquals(-1, idObjectifTest, "L'objectif créé doit exister");

        // Modifier
        ObjectifBienEtreDto modif = new ObjectifBienEtreDto();
        modif.setTitre("TestObjectif_ApresModif");
        modif.setType("poids");
        modif.setStatut("atteint");
        modif.setValeurCible(80.0);
        modif.setValeurActuelle(80.0);
        modif.setDateDebut(LocalDate.of(2025, 1, 1));
        modif.setDateFin(LocalDate.of(2025, 12, 31));
        modif.setUtilisateurId(1);
        service.update(idObjectifTest, modif);

        // Vérifier
        List<ObjectifBienEtreDto> apres = service.getAll();
        boolean titreModifie = apres.stream()
                .anyMatch(o -> "TestObjectif_ApresModif".equals(o.getTitre()));
        assertTrue(titreModifie, "Le titre modifié doit apparaître dans la liste");

        boolean ancienTitreDisparu = apres.stream()
                .noneMatch(o -> "TestObjectif_AvantModif".equals(o.getTitre()));
        assertTrue(ancienTitreDisparu, "L'ancien titre ne doit plus exister");
    }

    // ── Test 4 : Supprimer un objectif ────────────────────────────────────────
    @Test
    @Order(4)
    void testSupprimerObjectif() throws Exception {
        // Créer un objectif à supprimer
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setTitre("TestObjectif_ASupprimer");
        dto.setType("hydratation");
        dto.setStatut("en_cours");
        dto.setValeurCible(2.0);
        dto.setValeurActuelle(0.5);
        dto.setDateDebut(LocalDate.of(2025, 1, 1));
        dto.setDateFin(LocalDate.of(2025, 3, 31));
        dto.setUtilisateurId(1);
        service.create(dto);

        // Récupérer l'ID
        List<ObjectifBienEtreDto> avant = service.getAll();
        idObjectifTest = avant.stream()
                .filter(o -> "TestObjectif_ASupprimer".equals(o.getTitre()))
                .mapToInt(ObjectifBienEtreDto::getId)
                .max()
                .orElse(-1);

        assertNotEquals(-1, idObjectifTest, "L'objectif à supprimer doit exister");

        // Supprimer
        service.delete(idObjectifTest);

        // Vérifier qu'il n'existe plus
        List<ObjectifBienEtreDto> apres = service.getAll();
        boolean existeEncore = apres.stream()
                .anyMatch(o -> o.getId() == idObjectifTest);
        assertFalse(existeEncore, "L'objectif supprimé ne doit plus exister dans la liste");

        // Pas besoin que @AfterEach le supprime (déjà fait)
        idObjectifTest = -1;
    }

    // ── Test 5 : Validation — titre ne peut pas être null/vide ───────────────
    @Test
    @Order(5)
    void testAjouterObjectifSansTitre() {
        ObjectifBienEtreDto dto = new ObjectifBienEtreDto();
        dto.setTitre("");   // titre vide — doit échouer côté backend
        dto.setType("humeur");
        dto.setStatut("en_cours");
        dto.setValeurCible(10.0);
        dto.setDateDebut(LocalDate.of(2025, 1, 1));
        dto.setDateFin(LocalDate.of(2025, 12, 31));
        dto.setUtilisateurId(1);

        assertThrows(Exception.class, () -> service.create(dto),
                "Créer un objectif sans titre doit lever une exception");
    }
}