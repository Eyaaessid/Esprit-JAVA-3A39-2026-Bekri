package com.bekri.enums;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour les enums {@link UtilisateurStatut} et {@link UtilisateurRole}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnumsTest {

    // ══════════════════════════════════════════════════════════════════════════
    // UtilisateurStatut.fromDbValue()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("fromDbValue : 'actif' → ACTIF")
    void testFromDbValue_actif() {
        assertEquals(UtilisateurStatut.ACTIF, UtilisateurStatut.fromDbValue("actif"));
    }

    @Test
    @Order(2)
    @DisplayName("fromDbValue : 'bloque' → BLOQUE")
    void testFromDbValue_bloque() {
        assertEquals(UtilisateurStatut.BLOQUE, UtilisateurStatut.fromDbValue("bloque"));
    }

    @Test
    @Order(3)
    @DisplayName("fromDbValue : 'supprime' → SUPPRIME")
    void testFromDbValue_supprime() {
        assertEquals(UtilisateurStatut.SUPPRIME, UtilisateurStatut.fromDbValue("supprime"));
    }

    @Test
    @Order(4)
    @DisplayName("fromDbValue : 'inactif' → INACTIF")
    void testFromDbValue_inactif() {
        assertEquals(UtilisateurStatut.INACTIF, UtilisateurStatut.fromDbValue("inactif"));
    }

    @Test
    @Order(5)
    @DisplayName("fromDbValue : valeur inconnue → IllegalArgumentException")
    void testFromDbValue_inconnu() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UtilisateurStatut.fromDbValue("inconnu")
        );
    }

    @Test
    @Order(6)
    @DisplayName("fromDbValue : null → null retourné (pas d'exception)")
    void testFromDbValue_null() {
        assertNull(UtilisateurStatut.fromDbValue(null));
    }

    @Test
    @Order(7)
    @DisplayName("fromDbValue : espaces autour → valeur reconnue (trim implicite)")
    void testFromDbValue_avecEspaces() {
        assertEquals(UtilisateurStatut.ACTIF, UtilisateurStatut.fromDbValue("  actif  "));
    }

    @Test
    @Order(8)
    @DisplayName("getValue : chaque statut retourne sa valeur DB minuscule")
    void testGetValue_tousLesStatuts() {
        assertEquals("actif",    UtilisateurStatut.ACTIF.getValue());
        assertEquals("bloque",   UtilisateurStatut.BLOQUE.getValue());
        assertEquals("inactif",  UtilisateurStatut.INACTIF.getValue());
        assertEquals("supprime", UtilisateurStatut.SUPPRIME.getValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UtilisateurRole.fromJson()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(9)
    @DisplayName("UtilisateurRole.fromJson : 'user' → USER")
    void testRoleFromJson_user() {
        assertEquals(UtilisateurRole.USER, UtilisateurRole.fromJson("user"));
    }

    @Test
    @Order(10)
    @DisplayName("UtilisateurRole.fromJson : 'coach' → COACH")
    void testRoleFromJson_coach() {
        assertEquals(UtilisateurRole.COACH, UtilisateurRole.fromJson("coach"));
    }

    @Test
    @Order(11)
    @DisplayName("UtilisateurRole.fromJson : 'admin' → ADMIN")
    void testRoleFromJson_admin() {
        assertEquals(UtilisateurRole.ADMIN, UtilisateurRole.fromJson("admin"));
    }

    @Test
    @Order(12)
    @DisplayName("UtilisateurRole.fromJson : 'COACH' majuscules → COACH (insensible à la casse)")
    void testRoleFromJson_majuscules() {
        assertEquals(UtilisateurRole.COACH, UtilisateurRole.fromJson("COACH"));
    }

    @Test
    @Order(13)
    @DisplayName("UtilisateurRole.fromJson : null → null")
    void testRoleFromJson_null() {
        assertNull(UtilisateurRole.fromJson(null));
    }

    @Test
    @Order(14)
    @DisplayName("UtilisateurRole.fromJson : valeur inconnue → IllegalArgumentException")
    void testRoleFromJson_inconnu() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UtilisateurRole.fromJson("superadmin")
        );
    }

    @Test
    @Order(15)
    @DisplayName("UtilisateurRole.getValue : chaque rôle retourne sa valeur minuscule")
    void testRoleGetValue() {
        assertEquals("user",  UtilisateurRole.USER.getValue());
        assertEquals("coach", UtilisateurRole.COACH.getValue());
        assertEquals("admin", UtilisateurRole.ADMIN.getValue());
    }
}