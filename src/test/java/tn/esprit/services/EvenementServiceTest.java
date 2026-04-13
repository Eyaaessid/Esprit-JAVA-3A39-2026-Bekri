package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.models.Evenement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvenementServiceTest {
    
    static EvenementService service;
    static Evenement evenementTest;
    static int evenementTestId;
    
    @BeforeAll
    static void setup() {
        service = new EvenementService();
        evenementTest = new Evenement(
            "Conférence Tech 2024",
            "Une conférence sur les nouvelles technologies",
            LocalDate.of(2024, 6, 15),
            LocalDate.of(2024, 6, 17),
            "Tunis",
            200
        );
    }
    
    @Test
    @Order(1)
    void testAjouterEvenement() {
        assertDoesNotThrow(() -> {
            service.ajouterEvenement(evenementTest);
        });
        
        assertDoesNotThrow(() -> {
            List<Evenement> evenements = service.afficherEvenements();
            assertNotNull(evenements);
            assertFalse(evenements.isEmpty());
            
            // Récupérer l'ID du dernier événement ajouté
            Evenement dernierEvenement = evenements.get(evenements.size() - 1);
            evenementTestId = dernierEvenement.getId();
            evenementTest.setId(evenementTestId);
            
            assertEquals("Conférence Tech 2024", dernierEvenement.getNom());
            assertEquals("Tunis", dernierEvenement.getLieu());
        });
    }
    
    @Test
    @Order(2)
    void testAfficherEvenements() {
        assertDoesNotThrow(() -> {
            List<Evenement> evenements = service.afficherEvenements();
            assertNotNull(evenements);
            assertTrue(evenements.size() > 0);
        });
    }
    
    @Test
    @Order(3)
    void testGetEvenementById() {
        assertDoesNotThrow(() -> {
            Evenement evenement = service.getEvenementById(evenementTestId);
            assertNotNull(evenement);
            assertEquals(evenementTestId, evenement.getId());
            assertEquals("Conférence Tech 2024", evenement.getNom());
            assertEquals(200, evenement.getCapacite());
        });
    }
    
    @Test
    @Order(4)
    void testModifierEvenement() {
        assertDoesNotThrow(() -> {
            evenementTest.setNom("Conférence Tech 2024 - Modifié");
            evenementTest.setCapacite(250);
            service.modifierEvenement(evenementTest);
            
            Evenement evenementModifie = service.getEvenementById(evenementTestId);
            assertNotNull(evenementModifie);
            assertEquals("Conférence Tech 2024 - Modifié", evenementModifie.getNom());
            assertEquals(250, evenementModifie.getCapacite());
        });
    }
    
    @Test
    @Order(5)
    void testSupprimerEvenement() {
        assertDoesNotThrow(() -> {
            service.supprimerEvenement(evenementTestId);
            
            Evenement evenementSupprime = service.getEvenementById(evenementTestId);
            assertNull(evenementSupprime);
        });
    }
    
    @Test
    void testAjouterEvenementAvecDonneesInvalides() {
        Evenement evenementInvalide = new Evenement(
            null,
            "Description",
            LocalDate.now(),
            LocalDate.now(),
            "Lieu",
            100
        );
        
        assertThrows(SQLException.class, () -> {
            service.ajouterEvenement(evenementInvalide);
        });
    }
}
