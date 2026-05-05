package tn.esprit.community.model;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int postId;
    private int userId;
    private String authorNom;
    private String authorPrenom;
    private String authorRole;
    private String contenu;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAuthorNom() {
        return authorNom;
    }

    public void setAuthorNom(String authorNom) {
        this.authorNom = authorNom;
    }

    public String getAuthorPrenom() {
        return authorPrenom;
    }

    public void setAuthorPrenom(String authorPrenom) {
        this.authorPrenom = authorPrenom;
    }

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuthorDisplayName() {
        return authorPrenom + " " + authorNom;
    }

    public boolean canBeEditedBy(UserSummary user) {
        return user != null && (user.isAdmin() || user.id() == userId);
    }
}

