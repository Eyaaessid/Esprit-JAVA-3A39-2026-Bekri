package org.example.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.AppState;
import org.example.community.model.Comment;
import org.example.community.model.Post;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommunityApiServer {
    private HttpServer server;

    public void start(AppState appState, int port) throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/community/posts", exchange -> handlePosts(exchange, appState));
        server.createContext("/api/community/comments", exchange -> handleComments(exchange, appState));
        server.createContext("/api/community/dashboard", exchange -> handleDashboard(exchange, appState));
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handlePosts(HttpExchange exchange, AppState appState) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            int page = Math.max(1, parseInt(query.get("page"), 1));
            int limit = Math.min(100, Math.max(1, parseInt(query.get("limit"), 10)));
            String sort = query.getOrDefault("sort", "most_recent");
            String emotion = query.get("emotion");

            List<Post> posts = appState.getPostDao().findAllVisible(null, null);
            if (emotion != null && !emotion.isBlank()) {
                posts = posts.stream()
                        .filter(post -> emotion.equalsIgnoreCase(post.getEmotion()))
                        .toList();
            }

            Comparator<Post> comparator;
            switch (sort.toLowerCase(Locale.ROOT)) {
                case "most_liked" -> comparator = Comparator.comparing(Post::getLikesCount).reversed()
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder());
                case "most_commented" -> comparator = Comparator.comparing(Post::getCommentsCount).reversed()
                        .thenComparing(Post::getCreatedAt, Comparator.reverseOrder());
                default -> comparator = Comparator.comparing(Post::getCreatedAt).reversed();
            }
            posts = new ArrayList<>(posts);
            posts.sort(comparator);

            int total = posts.size();
            int from = Math.min((page - 1) * limit, total);
            int to = Math.min(from + limit, total);
            List<Post> pageItems = posts.subList(from, to);

            StringBuilder json = new StringBuilder();
            json.append("{\"items\":[");
            for (int i = 0; i < pageItems.size(); i++) {
                Post post = pageItems.get(i);
                if (i > 0) json.append(',');
                json.append('{')
                        .append("\"id\":").append(post.getId()).append(',')
                        .append("\"titre\":\"").append(escape(post.getTitre())).append("\",")
                        .append("\"contenu\":\"").append(escape(post.getContenu())).append("\",")
                        .append("\"categorie\":\"").append(escape(nullToEmpty(post.getCategorie()))).append("\",")
                        .append("\"emotion\":\"").append(escape(nullToEmpty(post.getEmotion()))).append("\",")
                        .append("\"risk_level\":\"").append(escape(nullToEmpty(post.getRiskLevel()))).append("\",")
                        .append("\"is_sensitive\":").append(post.isSensitive()).append(',')
                        .append("\"likes_count\":").append(post.getLikesCount()).append(',')
                        .append("\"comments_count\":").append(post.getCommentsCount())
                        .append('}');
            }
            json.append("],\"meta\":{")
                    .append("\"page\":").append(page).append(',')
                    .append("\"limit\":").append(limit).append(',')
                    .append("\"total\":").append(total).append(',')
                    .append("\"pages\":").append((int) Math.ceil(total / (double) limit)).append(',')
                    .append("\"sort\":\"").append(escape(sort)).append("\"")
                    .append("}}");

            send(exchange, 200, json.toString());
        } catch (SQLException exception) {
            send(exchange, 500, "{\"error\":\"" + escape(exception.getMessage()) + "\"}");
        }
    }

    private void handleComments(HttpExchange exchange, AppState appState) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI());
            int page = Math.max(1, parseInt(query.get("page"), 1));
            int limit = Math.min(100, Math.max(1, parseInt(query.get("limit"), 20)));
            Integer postId = query.containsKey("post_id") ? parseInt(query.get("post_id"), 0) : null;
            if (postId != null && postId <= 0) {
                postId = null;
            }
            String sort = query.getOrDefault("sort", "most_recent");

            List<Comment> comments = appState.getCommentDao().findVisible(postId, "oldest".equalsIgnoreCase(sort) ? "oldest" : "newest");
            int total = comments.size();
            int from = Math.min((page - 1) * limit, total);
            int to = Math.min(from + limit, total);
            List<Comment> pageItems = comments.subList(from, to);

            StringBuilder json = new StringBuilder();
            json.append("{\"items\":[");
            for (int i = 0; i < pageItems.size(); i++) {
                Comment comment = pageItems.get(i);
                if (i > 0) json.append(',');
                json.append('{')
                        .append("\"id\":").append(comment.getId()).append(',')
                        .append("\"post_id\":").append(comment.getPostId()).append(',')
                        .append("\"contenu\":\"").append(escape(comment.getContenu())).append("\",")
                        .append("\"created_at\":\"").append(escape(comment.getCreatedAt() == null ? "" : comment.getCreatedAt().toString())).append("\"")
                        .append('}');
            }
            json.append("],\"meta\":{")
                    .append("\"page\":").append(page).append(',')
                    .append("\"limit\":").append(limit).append(',')
                    .append("\"total\":").append(total).append(',')
                    .append("\"pages\":").append((int) Math.ceil(total / (double) limit))
                    .append("}}");
            send(exchange, 200, json.toString());
        } catch (SQLException exception) {
            send(exchange, 500, "{\"error\":\"" + escape(exception.getMessage()) + "\"}");
        }
    }

    private void handleDashboard(HttpExchange exchange, AppState appState) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            List<Post> posts = appState.getPostDao().findAllVisible(null, null);
            Map<String, Integer> emotionCounts = new HashMap<>();
            for (Post post : posts) {
                String emotion = nullToEmpty(post.getEmotion());
                if (emotion.isBlank()) {
                    emotion = "neutral";
                }
                emotionCounts.merge(emotion.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }

            StringBuilder json = new StringBuilder();
            json.append("{\"emotions\":[");
            int i = 0;
            for (Map.Entry<String, Integer> entry : emotionCounts.entrySet()) {
                if (i++ > 0) json.append(',');
                json.append("{\"emotion\":\"").append(escape(entry.getKey())).append("\",\"total\":").append(entry.getValue()).append('}');
            }
            json.append("]}");
            send(exchange, 200, json.toString());
        } catch (SQLException exception) {
            send(exchange, 500, "{\"error\":\"" + escape(exception.getMessage()) + "\"}");
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return map;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] split = pair.split("=", 2);
            String key = split.length > 0 ? split[0] : "";
            String value = split.length > 1 ? split[1] : "";
            map.put(key, value);
        }
        return map;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
