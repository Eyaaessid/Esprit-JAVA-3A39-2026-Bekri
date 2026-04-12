package com.bekri.services;

import com.bekri.entities.User;
import com.bekri.enums.UtilisateurStatut;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link JwtService}.
 *
 * Pas de mock nécessaire — JwtService ne dépend que de paramètres de config.
 * On instancie directement la classe avec des valeurs de test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtServiceTest {

    // Secret de 32+ caractères (requis par HS256)
    private static final String TEST_SECRET =
            "TestSecretKeyForJwtServiceTests_32chars!";
    private static final long EXPIRATION_SECONDS = 3600; // 1 heure

    private JwtService jwtService;
    private User utilisateurTest;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_SECONDS);

        utilisateurTest = new User();
        utilisateurTest.setId(42);
        utilisateurTest.setNom("Test");
        utilisateurTest.setPrenom("User");
        utilisateurTest.setEmail("jwt.test@bekri.com");
        utilisateurTest.setMotDePasse("hash");
        utilisateurTest.setStatut(UtilisateurStatut.ACTIF);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. generateToken()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("generateToken : retourne un token non null et non vide")
    void testGenerateToken_notNull() {
        String token = jwtService.generateToken(utilisateurTest);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @Order(2)
    @DisplayName("generateToken : token contient 3 parties séparées par un point (format JWT)")
    void testGenerateToken_formatJwt() {
        String token = jwtService.generateToken(utilisateurTest);

        // Un JWT valide = header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "Un JWT doit contenir exactement 3 parties");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. extractUserId()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("extractUserId : l'ID extrait correspond à l'ID de l'utilisateur")
    void testExtractUserId_correct() {
        String token = jwtService.generateToken(utilisateurTest);

        Integer extractedId = jwtService.extractUserId(token);

        assertEquals(42, extractedId);
    }

    @Test
    @Order(4)
    @DisplayName("extractUserId : token invalide → null retourné (pas d'exception)")
    void testExtractUserId_tokenInvalide() {
        Integer result = jwtService.extractUserId("token.invalide.xyz");

        assertNull(result);
    }

    @Test
    @Order(5)
    @DisplayName("extractUserId : chaîne vide → null retourné")
    void testExtractUserId_chaineVide() {
        Integer result = jwtService.extractUserId("");

        assertNull(result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. isTokenValid()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("isTokenValid : token fraîchement généré → true")
    void testIsTokenValid_tokenValide() {
        String token = jwtService.generateToken(utilisateurTest);

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @Order(7)
    @DisplayName("isTokenValid : token expiré → false")
    void testIsTokenValid_tokenExpire() {
        // Crée un service avec une expiration de 0 secondes (déjà expiré)
        JwtService expiredService = new JwtService(TEST_SECRET, 0L);
        String token = expiredService.generateToken(utilisateurTest);

        assertFalse(expiredService.isTokenValid(token));
    }

    @Test
    @Order(8)
    @DisplayName("isTokenValid : chaîne aléatoire → false (pas d'exception)")
    void testIsTokenValid_chaineAleatoire() {
        assertFalse(jwtService.isTokenValid("ceci.n.est.pas.un.jwt"));
    }

    @Test
    @Order(9)
    @DisplayName("isTokenValid : token signé avec un autre secret → false")
    void testIsTokenValid_mauvaisSecret() {
        // Token signé avec un secret différent
        JwtService autreService = new JwtService(
                "AutreSecretKeyCompletelyDifferent_32chars!", EXPIRATION_SECONDS);
        String tokenAutre = autreService.generateToken(utilisateurTest);

        // Validé avec le service principal → doit retourner false
        assertFalse(jwtService.isTokenValid(tokenAutre));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Cohérence generate → extract → validate
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("Flux complet : générer → valider → extraire l'ID → cohérence garantie")
    void testFluxComplet() {
        // 1. Générer
        String token = jwtService.generateToken(utilisateurTest);

        // 2. Valider
        assertTrue(jwtService.isTokenValid(token));

        // 3. Extraire l'ID
        Integer id = jwtService.extractUserId(token);
        assertEquals(utilisateurTest.getId(), id);
    }
}