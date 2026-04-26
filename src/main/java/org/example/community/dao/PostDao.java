package org.example.community.dao;

import org.example.community.model.Post;
import org.example.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PostDao {
    private static final String BASE_SELECT = """
            SELECT p.id, p.utilisateur_id, p.titre, p.contenu, p.media_url, p.categorie,
                   p.emotion, p.risk_level, p.is_sensitive, p.created_at, p.updated_at,
                   u.nom, u.prenom, u.role,
                   (SELECT COUNT(*) FROM commentaire c WHERE c.post_id = p.id AND c.deleted_at IS NULL) AS comments_count,
                   (SELECT COUNT(*) FROM `like` l WHERE l.post_id = p.id) AS likes_count
            FROM post p
            INNER JOIN utilisateur u ON u.id = p.utilisateur_id
            WHERE p.deleted_at IS NULL
            """;

    public List<Post> findAllVisible(String search, String category) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> parameters = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(p.titre) LIKE ? OR LOWER(p.contenu) LIKE ? OR LOWER(COALESCE(p.categorie, '')) LIKE ?)");
            String keyword = "%" + search.toLowerCase() + "%";
            parameters.add(keyword);
            parameters.add(keyword);
            parameters.add(keyword);
        }

        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            sql.append(" AND p.categorie = ?");
            parameters.add(category);
        }

        sql.append(" ORDER BY p.created_at DESC");

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Post> posts = new ArrayList<>();
                while (resultSet.next()) {
                    posts.add(mapPost(resultSet));
                }
                return posts;
            }
        }
    }

    public java.util.Optional<Post> findVisibleById(int postId) throws SQLException {
        String sql = BASE_SELECT + " AND p.id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return java.util.Optional.of(mapPost(resultSet));
                }
                return java.util.Optional.empty();
            }
        }
    }

    public Post insert(Post post) throws SQLException {
        String sql = """
                INSERT INTO post (titre, contenu, media_url, categorie, emotion, risk_level, is_sensitive, created_at, utilisateur_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, post.getTitre());
            statement.setString(2, post.getContenu());
            statement.setString(3, emptyToNull(post.getMediaUrl()));
            statement.setString(4, emptyToNull(post.getCategorie()));
            statement.setString(5, emptyToNull(post.getEmotion()));
            statement.setString(6, post.getRiskLevel() == null ? "low" : post.getRiskLevel());
            statement.setBoolean(7, post.isSensitive());
            statement.setInt(8, post.getUserId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setId(keys.getInt(1));
                }
            }
        }
        return post;
    }

    public void update(Post post) throws SQLException {
        String sql = """
                UPDATE post
                SET titre = ?, contenu = ?, media_url = ?, categorie = ?, emotion = ?, risk_level = ?, is_sensitive = ?, updated_at = NOW()
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, post.getTitre());
            statement.setString(2, post.getContenu());
            statement.setString(3, emptyToNull(post.getMediaUrl()));
            statement.setString(4, emptyToNull(post.getCategorie()));
            statement.setString(5, emptyToNull(post.getEmotion()));
            statement.setString(6, post.getRiskLevel() == null ? "low" : post.getRiskLevel());
            statement.setBoolean(7, post.isSensitive());
            statement.setInt(8, post.getId());
            statement.executeUpdate();
        }
    }

    public void softDelete(int postId) throws SQLException {
        String sql = "UPDATE post SET deleted_at = NOW() WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, postId);
            statement.executeUpdate();
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    public List<Post> findVisibleByIds(List<Integer> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = BASE_SELECT + " AND p.id IN (" + placeholders + ")";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                statement.setInt(i + 1, ids.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Post> posts = new ArrayList<>();
                while (resultSet.next()) {
                    posts.add(mapPost(resultSet));
                }
                return posts;
            }
        }
    }

    public List<Integer> findPostIdsByAuthor(int userId) throws SQLException {
        String sql = "SELECT id FROM post WHERE utilisateur_id = ? AND deleted_at IS NULL";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getInt("id"));
                }
                return ids;
            }
        }
    }

    public List<String> findDistinctCategoriesByAuthor(int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT categorie
                FROM post
                WHERE utilisateur_id = ?
                  AND deleted_at IS NULL
                  AND categorie IS NOT NULL
                  AND TRIM(categorie) <> ''
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> categories = new ArrayList<>();
                while (resultSet.next()) {
                    categories.add(resultSet.getString("categorie"));
                }
                return categories;
            }
        }
    }

    public List<Post> findByCategories(List<String> categories, List<Integer> excludedPostIds, Integer excludeAuthorId, int limit) throws SQLException {
        if (categories == null || categories.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<Post> all = findAllVisible(null, null);
        Set<String> normalized = categories.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return all.stream()
                .filter(post -> post.getCategorie() != null && normalized.contains(post.getCategorie().toLowerCase(Locale.ROOT)))
                .filter(post -> excludedPostIds == null || !excludedPostIds.contains(post.getId()))
                .filter(post -> excludeAuthorId == null || post.getUserId() != excludeAuthorId)
                .sorted(Comparator.comparing(Post::getCommentsCount).reversed()
                        .thenComparing(Post::getLikesCount, Comparator.reverseOrder())
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    public List<Post> findMostPopularRecent(int limit, List<Integer> excludedPostIds, Integer excludeAuthorId) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        List<Post> all = findAllVisible(null, null);
        return all.stream()
                .filter(post -> excludedPostIds == null || !excludedPostIds.contains(post.getId()))
                .filter(post -> excludeAuthorId == null || post.getUserId() != excludeAuthorId)
                .sorted(Comparator.comparing(Post::getLikesCount).reversed()
                        .thenComparing(Post::getCommentsCount, Comparator.reverseOrder())
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    public List<Post> findRelatedToPost(int excludedPostId, String category, Integer authorId, int limit) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        List<Post> all = findAllVisible(null, null);
        List<Post> filtered = all.stream()
                .filter(post -> post.getId() != excludedPostId)
                .filter(post -> {
                    boolean sameCategory = category != null && !category.isBlank() && category.equalsIgnoreCase(post.getCategorie());
                    boolean sameAuthor = authorId != null && authorId == post.getUserId();
                    return sameCategory || sameAuthor;
                })
                .sorted(Comparator.comparing(Post::getCreatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();

        if (!filtered.isEmpty()) {
            return filtered;
        }

        return findMostPopularRecent(limit, List.of(excludedPostId), null);
    }

    private Post mapPost(ResultSet resultSet) throws SQLException {
        Post post = new Post();
        post.setId(resultSet.getInt("id"));
        post.setUserId(resultSet.getInt("utilisateur_id"));
        post.setTitre(resultSet.getString("titre"));
        post.setContenu(resultSet.getString("contenu"));
        post.setMediaUrl(resultSet.getString("media_url"));
        post.setCategorie(resultSet.getString("categorie"));
        post.setEmotion(resultSet.getString("emotion"));
        post.setRiskLevel(resultSet.getString("risk_level"));
        post.setSensitive(resultSet.getBoolean("is_sensitive"));
        post.setAuthorNom(resultSet.getString("nom"));
        post.setAuthorPrenom(resultSet.getString("prenom"));
        post.setAuthorRole(resultSet.getString("role"));
        post.setLikesCount(resultSet.getInt("likes_count"));
        post.setCommentsCount(resultSet.getInt("comments_count"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            post.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            post.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return post;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
