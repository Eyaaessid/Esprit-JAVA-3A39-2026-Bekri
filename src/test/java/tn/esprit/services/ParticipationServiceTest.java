package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.models.ParticipationEvenement;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParticipationServiceTest {
    
    private static ParticipationService participationService;
    private static ParticipationEvenement testParticipation;
    private static int testParticipationId;
    
    @BeforeAll
    static void setUp() {
        System.out.println("=== Initialisation des tests ParticipationService ===");
        participationService = new ParticipationService();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Ajouter une participation")
    void testAjouterParticipation() {
        System.out.println("\n--- Test 1: Ajouter une participation ---");
        
        // Créer une participation de test
        testParticipation = new ParticipationEvenement();
        testParticipation.setDate_inscription(LocalDateTime.now());
        testParticipation.setStatut("confirmé");
        testParticipation.setCommentaire("Participation de test créée par JUnit");
        testParticipation.setUtilisateur_id(1); // ID utilisateur de test
        testParticipation.setEvenement_id(1);   // ID événement de test (doit exister en base)
        
        // Ajouter la participation
        participationService.ajouter(testParticipation);
        
        // Vérifications
        assertNotNull(testParticipation, "La participation ne doit pas être null");
        assertTrue(testParticipation.getId() > 0, "L'ID doit être généré et supérieur à 0");
        
        testParticipationId = testParticipation.getId();
        System.out.println("✓ Participation ajoutée avec ID: " + testParticipationId);
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Afficher toutes les participations")
    void testAfficherAll() {
        System.out.println("\n--- Test 2: Afficher toutes les participations ---");
        
        // Récupérer toutes les participations
        List<ParticipationEvenement> participations = participationService.afficherAll();
        
        // Vérifications
        assertNotNull(participations, "La liste ne doit pas être null");
        assertFalse(participations.isEmpty(), "La liste ne doit pas être vide");
        
        // Vérifier que notre participation de test est dans la liste
        boolean found = participations.stream()
                .anyMatch(p -> p.getId() == testParticipationId);
        
        assertTrue(found, "La participation de test doit être présente dans la liste");
        System.out.println("✓ Nombre de participations récupérées: " + participations.size());
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Modifier une participation")
    void testModifierParticipation() {
        System.out.println("\n--- Test 3: Modifier une participation ---");
        
        // Récupérer la participation de test
        List<ParticipationEvenement> participations = participationService.afficherAll();
        ParticipationEvenement participationAModifier = participations.stream()
                .filter(p -> p.getId() == testParticipationId)
                .findFirst()
                .orElse(null);
        
        assertNotNull(participationAModifier, "La participation à modifier doit exister");
        
        // Modifier les données
        String nouveauStatut = "présent";
        String nouveauCommentaire = "Participation MODIFIÉE par JUnit";
        
        participationAModifier.setStatut(nouveauStatut);
        participationAModifier.setCommentaire(nouveauCommentaire);
        
        // Appliquer la modification
        participationService.modifier(participationAModifier);
        
        // Vérifier la modification
        List<ParticipationEvenement> participationsApresModif = participationService.afficherAll();
        ParticipationEvenement participationModifiee = participationsApresModif.stream()
                .filter(p -> p.getId() == testParticipationId)
                .findFirst()
                .orElse(null);
        
        assertNotNull(participationModifiee, "La participation modifiée doit exister");
        assertEquals(nouveauStatut, participationModifiee.getStatut(), "Le statut doit être modifié");
        assertEquals(nouveauCommentaire, participationModifiee.getCommentaire(), "Le commentaire doit être modifié");
        
        System.out.println("✓ Participation modifiée avec succès");
        System.out.println("  - Nouveau statut: " + participationModifiee.getStatut());
        System.out.println("  - Nouveau commentaire: " + participationModifiee.getCommentaire());
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Supprimer une participation")
    void testSupprimerParticipation() {
        System.out.println("\n--- Test 4: Supprimer une participation ---");
        
        // Compter les participations avant suppression
        List<ParticipationEvenement> participationsAvant = participationService.afficherAll();
        int nombreAvant = participationsAvant.size();
        
        // Supprimer la participation de test
        participationService.supprimer(testParticipationId);
        
        // Compter les participations après suppression
        List<ParticipationEvenement> participationsApres = participationService.afficherAll();
        int nombreApres = participationsApres.size();
        
        // Vérifications
        assertEquals(nombreAvant - 1, nombreApres, "Le nombre de participations doit diminuer de 1");
        
        // Vérifier que la participation n'existe plus
        boolean found = participationsApres.stream()
                .anyMatch(p -> p.getId() == testParticipationId);
        
        assertFalse(found, "La participation supprimée ne doit plus être dans la liste");
        
        System.out.println("✓ Participation supprimée avec succès");
        System.out.println("  - Nombre avant: " + nombreAvant);
        System.out.println("  - Nombre après: " + nombreApres);
    }
    
    @AfterAll
    static void tearDown() {
        System.out.println("\n=== Fin des tests ParticipationService ===");
        System.out.println("Tous les tests ont été exécutés avec succès !");
    }
}
