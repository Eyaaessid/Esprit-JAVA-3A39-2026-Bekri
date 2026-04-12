package com.bekri.entities;

import com.bekri.enums.UtilisateurStatut;
import org.junit.jupiter.api.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la logique métier de {@link Utilisateur}
 * et ses sous-classes {@link User}, {@link Coach}, {@link Admin}.
 *
 * Aucune dépendance Spring/JPA — tests purs Java.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UtilisateurTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. getRoleValue()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("getRoleValue : champ role null → retourne 'user' par défaut")
    void testGetRoleValue_nullRetourneUser() {
        User u = new User();
        // role non renseigné (null) → valeur par défaut
        assertEquals("user", u.getRoleValue());
    }

    @Test
    @Order(2)
    @DisplayName("getRoleValue : champ role blanc → retourne 'user' par défaut")
    void testGetRoleValue_blancRetourneUser() {
        User u = new User();
        u.setRole("   ");
        assertEquals("user", u.getRoleValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. getAuthorities()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("getAuthorities : role=user → ROLE_USER seulement")
    void testAuthorities_user() {
        User u = new User();
        Collection<? extends GrantedAuthority> auth = u.getAuthorities();

        assertEquals(1, auth.size());
        assertTrue(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertFalse(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_COACH")));
    }

    @Test
    @Order(4)
    @DisplayName("getAuthorities : role=coach → ROLE_USER + ROLE_COACH")
    void testAuthorities_coach() {
        Coach c = new Coach();
        c.setRole("coach");
        Collection<? extends GrantedAuthority> auth = c.getAuthorities();

        assertEquals(2, auth.size());
        assertTrue(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_COACH")));
        assertFalse(auth.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @Order(5)
    @DisplayName("getAuthorities : role=admin → ROLE_USER + ROLE_ADMIN")
    void testAuthorities_admin() {
        Admin a = new Admin();
        a.setRole("admin");
        Collection<? extends GrantedAuthority> auth = a.getAuthorities();

        assertEquals(2, auth.size());
        assertTrue(auth.stream().anyMatch(x -> x.getAuthority().equals("ROLE_USER")));
        assertTrue(auth.stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(auth.stream().anyMatch(x -> x.getAuthority().equals("ROLE_COACH")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. isEnabled() / isAccountNonLocked()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("isEnabled : statut ACTIF → true")
    void testIsEnabled_actif() {
        User u = new User();
        u.setStatut(UtilisateurStatut.ACTIF);
        assertTrue(u.isEnabled());
    }

    @Test
    @Order(7)
    @DisplayName("isEnabled : statut SUPPRIME → false")
    void testIsEnabled_supprime() {
        User u = new User();
        u.setStatut(UtilisateurStatut.SUPPRIME);
        assertFalse(u.isEnabled());
    }

    @Test
    @Order(8)
    @DisplayName("isAccountNonLocked : statut BLOQUE → false (compte verrouillé)")
    void testIsAccountNonLocked_bloque() {
        User u = new User();
        u.setStatut(UtilisateurStatut.BLOQUE);
        assertFalse(u.isAccountNonLocked());
    }

    @Test
    @Order(9)
    @DisplayName("isAccountNonLocked : statut ACTIF → true")
    void testIsAccountNonLocked_actif() {
        User u = new User();
        u.setStatut(UtilisateurStatut.ACTIF);
        assertTrue(u.isAccountNonLocked());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. getUsername() / getPassword()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @DisplayName("getUsername : retourne l'email")
    void testGetUsername_retourneEmail() {
        User u = new User();
        u.setEmail("user@bekri.com");
        assertEquals("user@bekri.com", u.getUsername());
    }

    @Test
    @Order(11)
    @DisplayName("getPassword : retourne le mot de passe hashé")
    void testGetPassword_retourneMotDePasse() {
        User u = new User();
        u.setMotDePasse("$2a$12$hashedPassword");
        assertEquals("$2a$12$hashedPassword", u.getPassword());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. copyPersistableStateTo()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(12)
    @DisplayName("copyPersistableStateTo : tous les champs métier sont copiés")
    void testCopyPersistableStateTo() {
        // Source
        User source = new User();
        source.setId(1);
        source.setNom("Original");
        source.setPrenom("Prenom");
        source.setEmail("original@bekri.com");
        source.setMotDePasse("hash");
        source.setStatut(UtilisateurStatut.ACTIF);

        // Cible (transition vers Coach par exemple)
        Coach target = new Coach();
        source.copyPersistableStateTo(target);

        // Assert : champs copiés
        assertEquals("Original", target.getNom());
        assertEquals("Prenom", target.getPrenom());
        assertEquals("original@bekri.com", target.getEmail());
        assertEquals("hash", target.getMotDePasse());
        assertEquals(UtilisateurStatut.ACTIF, target.getStatut());

        // Assert : ID non copié (doit rester null sur la cible)
        assertNull(target.getId());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Valeurs par défaut des booléens
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(13)
    @DisplayName("Valeurs par défaut : isVerified=false, faceAuthEnabled=false, isTwoFactorEnabled=false")
    void testValeursParDefaut() {
        User u = new User();
        assertFalse(u.isVerified());
        assertFalse(u.isFaceAuthEnabled());
        assertFalse(u.isTwoFactorEnabled());
        assertEquals(0, u.getFaceAuthFailedAttempts());
    }
}