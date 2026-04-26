package org.example.community.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentTest {

    @Test
    void shouldStoreAndReturnAllCommentFields() {
        Comment comment = new Comment();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 13, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 13, 9, 15);

        comment.setId(3);
        comment.setPostId(11);
        comment.setUserId(7);
        comment.setAuthorNom("Bekri");
        comment.setAuthorPrenom("Ahmed");
        comment.setAuthorRole("user");
        comment.setContenu("Commentaire de test");
        comment.setCreatedAt(createdAt);
        comment.setUpdatedAt(updatedAt);

        assertEquals(3, comment.getId());
        assertEquals(11, comment.getPostId());
        assertEquals(7, comment.getUserId());
        assertEquals("Bekri", comment.getAuthorNom());
        assertEquals("Ahmed", comment.getAuthorPrenom());
        assertEquals("user", comment.getAuthorRole());
        assertEquals("Commentaire de test", comment.getContenu());
        assertEquals(createdAt, comment.getCreatedAt());
        assertEquals(updatedAt, comment.getUpdatedAt());
    }

    @Test
    void shouldBuildAuthorDisplayName() {
        Comment comment = new Comment();
        comment.setAuthorPrenom("Ahmed");
        comment.setAuthorNom("Bekri");

        assertEquals("Ahmed Bekri", comment.getAuthorDisplayName());
    }

    @Test
    void shouldAllowEditionForOwner() {
        Comment comment = new Comment();
        comment.setUserId(7);
        UserSummary owner = new UserSummary(7, "Bekri", "Ahmed", "user");

        assertTrue(comment.canBeEditedBy(owner));
    }

    @Test
    void shouldAllowEditionForAdmin() {
        Comment comment = new Comment();
        comment.setUserId(7);
        UserSummary admin = new UserSummary(99, "Admin", "Super", "admin");

        assertTrue(comment.canBeEditedBy(admin));
    }

    @Test
    void shouldRefuseEditionForAnotherUser() {
        Comment comment = new Comment();
        comment.setUserId(7);
        UserSummary otherUser = new UserSummary(12, "Ali", "Test", "user");

        assertFalse(comment.canBeEditedBy(otherUser));
    }

    @Test
    void shouldRefuseEditionWhenUserIsNull() {
        Comment comment = new Comment();
        comment.setUserId(7);

        assertFalse(comment.canBeEditedBy(null));
    }
}
