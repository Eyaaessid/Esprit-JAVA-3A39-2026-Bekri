package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.models.Evenement;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvenementServiceTest {
    
    private static EvenementService evenementService;
    private static Evenement testEvenement;
    private static int testEvenementId;
    
    @BeforeAll
    static void setUp() {
        System.out.println("=== Initialisation des tests EvenementService ===");
        evenementService = new EvenementService();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Ajouter un événement")
    void testAjouterEvenement() {
        System.out.println("\n--- Test 1: Ajouter un événement ---");
        
        // Créer un événement de test
        testEvenement = new Evenement();
        testEvenement.setTitre("Test JUnit - Événement");
        testEvenement.setDescription("Ceci est un événement de test créé par JUnit");
        testEvenement.setDate_debut(LocalDateTime.now().plusDays(1));
        testEvenement.setDate_fin(LocalDateTime.now().plusDays(2));
        testEvenement.setLieu("Salle de test");
        testEvenement.setCapacite_max(50);
        testEvenement.setType("test");
        testEvenement.setStatut("planifié");
        testEvenement.setImage("");
        testEvenement.setCreated_at(LocalDateTime.now());
        testEvenement.setCoach_id(1);
        
        // Ajouter l'événement
        evenementService.ajouter(testEvenement);
        
        // Vérifications
        assertNotNull(testEvenement, "L'événement ne doit pas être null");
        assertTrue(testEvenement.getId() > 0, "L'ID doit être généré et supérieur à 0");
        
        testEvenementId = testEvenement.getId();
        System.out.println("✓ Événement ajouté avec ID: " + testEvenementId);
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Afficher tous les événements")
    void testAfficherAll() {
        System.out.println("\n--- Test 2: Afficher tous les événements ---");
        
        // Récupérer tous les événements
        List<Evenement> evenements = evenementService.afficherAll();
        
        // Vérifications
        assertNotNull(evenements, "La liste ne doit pas être null");
        assertFalse(evenements.isEmpty(), "La liste ne doit pas être vide");
        
        // Vérifier que notre événement de test est dans la liste
        boolean found = evenements.stream()
                .anyMatch(e -> e.getId() == testEvenementId);
        
        assertTrue(found, "L'événement de test doit être présent dans la liste");
        System.out.println("✓ Nombre d'événements récupérés: " + evenements.size());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Modifier un événement")
    void testModifierEvenement() {
        System.out.println("\n--- Test 3: Modifier un événement ---");
        
        // Récupérer l'événement de test
        List<Evenement> evenements = evenementService.afficherAll();
        Evenement evenementAModifier = evenements.stream()
                .filter(e -> e.getId() == testEvenementId)
                .findFirst()
                .orElse(null);
        
        assertNotNull(evenementAModifier, "L'événement à modifier doit exister");
        
        // Modifier les données
        String nouveauTitre = "Test JUnit - Événement MODIFIÉ";
        String nouveauStatut = "en cours";
        int nouvelleCapacite = 100;
        
        evenementAModifier.setTitre(nouveauTitre);
        evenementAModifier.setStatut(nouveauStatut);
        evenementAModifier.setCapacite_max(nouvelleCapacite);
        
        // Appliquer la modification
        evenementService.modifier(evenementAModifier);
        
        // Vérifier la modification
        List<Evenement> evenementsApresModif = evenementService.afficherAll();
        Evenement evenementModifie = evenementsApresModif.stream()
                .filter(e -> e.getId() == testEvenementId)
                .findFirst()
                .orElse(null);
        
        assertNotNull(evenementModifie, "L'événement modifié doit exister");
        assertEquals(nouveauTitre, evenementModifie.getTitre(), "Le titre doit être modifié");
        assertEquals(nouveauStatut, evenementModifie.getStatut(), "Le statut doit être modifié");
        assertEquals(nouvelleCapacite, evenementModifie.getCapacite_max(), "La capacité doit être modifiée");
        
        System.out.println("✓ Événement modifié avec succès");
        System.out.println("  - Nouveau titre: " + evenementModifie.getTitre());
        System.out.println("  - Nouveau statut: " + evenementModifie.getStatut());
        System.out.println("  - Nouvelle capacité: " + evenementModifie.getCapacite_max());
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Supprimer un événement")
    void testSupprimerEvenement() {
        System.out.println("\n--- Test 4: Supprimer un événement ---");
        
        // Compter les événements avant suppression
        List<Evenement> evenementsAvant = evenementService.afficherAll();
        int nombreAvant = evenementsAvant.size();
        
        // Supprimer l'événement de test
        evenementService.supprimer(testEvenementId);
        
        // Compter les événements après suppression
        List<Evenement> evenementsApres = evenementService.afficherAll();
        int nombreApres = evenementsApres.size();
        
        // Vérifications
        assertEquals(nombreAvant - 1, nombreApres, "Le nombre d'événements doit diminuer de 1");
        
        // Vérifier que l'événement n'existe plus
        boolean found = evenementsApres.stream()
                .anyMatch(e -> e.getId() == testEvenementId);
        
        assertFalse(found, "L'événement supprimé ne doit plus être dans la liste");
        
        System.out.println("✓ Événement supprimé avec succès");
        System.out.println("  - Nombre avant: " + nombreAvant);
        System.out.println("  - Nombre après: " + nombreApres);
    }
    
    @AfterAll
    static void tearDown() {
        System.out.println("\n=== Fin des tests EvenementService ===");
        System.out.println("Tous les tests ont été exécutés avec succès !");
    }
}
