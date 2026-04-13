package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.models.Evenement;
import tn.esprit.models.ParticipationDisplay;
import tn.esprit.models.ParticipationEvenement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ParticipationServiceTest {
    
    static ParticipationService participationService;
    static EvenementService evenementService;
    static ParticipationEvenement participationTest;
    static int participationTestId;
    static int evenementTestId;
    
    @BeforeAll
    static void setup() {
        participationService = new ParticipationService();
        evenementService = new EvenementService();
        
        // Créer un événement pour les tests
        try {
            Evenement evenementTest = new Evenement(
                "Événement Test Participation",
                "Description test",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12),
                "Sfax",
                100
            );
            evenementService.ajouterEvenement(evenementTest);
            
            List<Evenement> evenements = evenementService.afficherEvenements();
            evenementTestId = evenements.get(evenements.size() - 1).getId();
            
            participationTest = new ParticipationEvenement(
                evenementTestId,
                "Ahmed Ben Ali",
                "ahmed.benali@example.com",
                "Confirmé"
            );
        } catch (SQLException e) {
            fail("Erreur lors de la configuration des tests: " + e.getMessage());
        }
    }
    
    @Test
    @Order(1)
    void testAjouterParticipation() {
        assertDoesNotThrow(() -> {
            participationService.ajouterParticipation(participationTest);
        });
        
        assertDoesNotThrow(() -> {
            List<ParticipationDisplay> participations = participationService.afficherParticipations();
            assertNotNull(participations);
            assertFalse(participations.isEmpty());
            
            // Trouver la participation ajoutée
            ParticipationDisplay derniereParticipation = participations.stream()
                .filter(p -> p.getNomParticipant().equals("Ahmed Ben Ali"))
                .findFirst()
                .orElse(null);
            
            assertNotNull(derniereParticipation);
            participationTestId = derniereParticipation.getParticipationId();
            participationTest.setId(participationTestId);
            
            assertEquals("Ahmed Ben Ali", derniereParticipation.getNomParticipant());
            assertEquals("ahmed.benali@example.com", derniereParticipation.getEmailParticipant());
            assertEquals("Confirmé", derniereParticipation.getStatut());
        });
    }
    
    @Test
    @Order(2)
    void testAfficherParticipations() {
        assertDoesNotThrow(() -> {
            List<ParticipationDisplay> participations = participationService.afficherParticipations();
            assertNotNull(participations);
            assertTrue(participations.size() > 0);
            
            // Vérifier que les données de l'événement sont bien jointes
            ParticipationDisplay participation = participations.stream()
                .filter(p -> p.getParticipationId() == participationTestId)
                .findFirst()
                .orElse(null);
            
            assertNotNull(participation);
            assertNotNull(participation.getNomEvenement());
            assertNotNull(participation.getLieuEvenement());
        });
    }
    
    @Test
    @Order(3)
    void testGetParticipationById() {
        assertDoesNotThrow(() -> {
            ParticipationEvenement participation = participationService.getParticipationById(participationTestId);
            assertNotNull(participation);
            assertEquals(participationTestId, participation.getId());
            assertEquals("Ahmed Ben Ali", participation.getNomParticipant());
            assertEquals("ahmed.benali@example.com", participation.getEmailParticipant());
            assertEquals("Confirmé", participation.getStatut());
            assertEquals(evenementTestId, participation.getEvenementId());
        });
    }
    
    @Test
    @Order(4)
    void testModifierParticipation() {
        assertDoesNotThrow(() -> {
            participationTest.setNomParticipant("Ahmed Ben Ali - Modifié");
            participationTest.setStatut("En attente");
            participationService.modifierParticipation(participationTest);
            
            ParticipationEvenement participationModifiee = participationService.getParticipationById(participationTestId);
            assertNotNull(participationModifiee);
            assertEquals("Ahmed Ben Ali - Modifié", participationModifiee.getNomParticipant());
            assertEquals("En attente", participationModifiee.getStatut());
        });
    }
    
    @Test
    @Order(5)
    void testSupprimerParticipation() {
        assertDoesNotThrow(() -> {
            participationService.supprimerParticipation(participationTestId);
            
            ParticipationEvenement participationSupprimee = participationService.getParticipationById(participationTestId);
            assertNull(participationSupprimee);
        });
    }
    
    @Test
    void testAjouterParticipationAvecEmailInvalide() {
        ParticipationEvenement participationInvalide = new ParticipationEvenement(
            evenementTestId,
            "Test User",
            null,
            "Confirmé"
        );
        
        assertThrows(SQLException.class, () -> {
            participationService.ajouterParticipation(participationInvalide);
        });
    }
    
    @AfterAll
    static void cleanup() {
        // Nettoyer l'événement de test
        try {
            evenementService.supprimerEvenement(evenementTestId);
        } catch (SQLException e) {
            System.err.println("Erreur lors du nettoyage: " + e.getMessage());
        }
    }
}
