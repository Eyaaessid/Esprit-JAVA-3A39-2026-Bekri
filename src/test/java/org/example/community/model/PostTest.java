package org.example.community.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostTest {

    @Test
    void shouldStoreAndReturnAllPostFields() {
        Post post = new Post();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 11, 45);

        post.setId(1);
        post.setUserId(7);
        post.setAuthorNom("Bekri");
        post.setAuthorPrenom("Ahmed");
        post.setAuthorRole("user");
        post.setTitre("Titre test");
        post.setContenu("Contenu test");
        post.setMediaUrl("/uploads/post.png");
        post.setCategorie("Actualite");
        post.setEmotion("Happy");
        post.setRiskLevel("Low");
        post.setSensitive(true);
        post.setCreatedAt(createdAt);
        post.setUpdatedAt(updatedAt);
        post.setLikesCount(14);
        post.setCommentsCount(5);

        assertEquals(1, post.getId());
        assertEquals(7, post.getUserId());
        assertEquals("Bekri", post.getAuthorNom());
        assertEquals("Ahmed", post.getAuthorPrenom());
        assertEquals("user", post.getAuthorRole());
        assertEquals("Titre test", post.getTitre());
        assertEquals("Contenu test", post.getContenu());
        assertEquals("/uploads/post.png", post.getMediaUrl());
        assertEquals("Actualite", post.getCategorie());
        assertEquals("Happy", post.getEmotion());
        assertEquals("Low", post.getRiskLevel());
        assertTrue(post.isSensitive());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(updatedAt, post.getUpdatedAt());
        assertEquals(14, post.getLikesCount());
        assertEquals(5, post.getCommentsCount());
    }

    @Test
    void shouldBuildAuthorDisplayName() {
        Post post = new Post();
        post.setAuthorPrenom("Ahmed");
        post.setAuthorNom("Bekri");

        assertEquals("Ahmed Bekri", post.getAuthorDisplayName());
    }

    @Test
    void shouldAllowEditionForOwner() {
        Post post = new Post();
        post.setUserId(7);
        UserSummary owner = new UserSummary(7, "Bekri", "Ahmed", "user");

        assertTrue(post.canBeEditedBy(owner));
    }

    @Test
    void shouldAllowEditionForAdmin() {
        Post post = new Post();
        post.setUserId(7);
        UserSummary admin = new UserSummary(99, "Admin", "Super", "admin");

        assertTrue(post.canBeEditedBy(admin));
    }

    @Test
    void shouldRefuseEditionForAnotherUser() {
        Post post = new Post();
        post.setUserId(7);
        UserSummary otherUser = new UserSummary(10, "Ali", "Test", "user");

        assertFalse(post.canBeEditedBy(otherUser));
    }

    @Test
    void shouldRefuseEditionWhenUserIsNull() {
        Post post = new Post();
        post.setUserId(7);

        assertFalse(post.canBeEditedBy(null));
    }
}
