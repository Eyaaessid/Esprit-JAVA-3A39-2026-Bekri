package tn.esprit.community.service;

import tn.esprit.community.dao.LikeDao;
import tn.esprit.community.dao.PostDao;
import tn.esprit.community.dao.SavedPostDao;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.UserSummary;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PostRecommendationService {
    private final PostDao postDao;
    private final LikeDao likeDao;
    private final SavedPostDao savedPostDao;

    public PostRecommendationService(PostDao postDao, LikeDao likeDao, SavedPostDao savedPostDao) {
        this.postDao = postDao;
        this.likeDao = likeDao;
        this.savedPostDao = savedPostDao;
    }

    public List<Post> getRecommendedForUser(UserSummary user, int limit, List<Integer> excludedFeedIds) throws SQLException {
        return getRecommendedForUser(user, limit, excludedFeedIds, false);
    }

    public List<Post> getRecommendedForUser(UserSummary user, int limit, List<Integer> excludedFeedIds, boolean prioritizeSavedAuthors) throws SQLException {
        if (limit <= 0) {
            return List.of();
        }

        if (user == null) {
            return postDao.findMostPopularRecent(limit, excludedFeedIds, null);
        }

        Set<Integer> excludeIds = new LinkedHashSet<>(excludedFeedIds == null ? List.of() : excludedFeedIds);
        List<Integer> likedIds = likeDao.findLikedPostIdsByUser(user.id());
        List<Integer> savedIds = savedPostDao.findSavedPostIdsByUser(user.id());
        List<Integer> ownPostIds = postDao.findPostIdsByAuthor(user.id());

        excludeIds.addAll(likedIds);
        excludeIds.addAll(savedIds);
        excludeIds.addAll(ownPostIds);

        List<Post> recommendations = new ArrayList<>();
        if (!savedIds.isEmpty()) {
            List<Post> savedPosts = postDao.findVisibleByIds(savedIds);
            List<Integer> savedAuthors = savedPosts.stream()
                    .map(Post::getUserId)
                    .distinct()
                    .filter(authorId -> authorId != user.id())
                    .toList();
            if (!savedAuthors.isEmpty()) {
                recommendations.addAll(postDao.findByAuthors(savedAuthors, new ArrayList<>(excludeIds), user.id(), limit));
                recommendations.forEach(post -> excludeIds.add(post.getId()));
            }
        }

        if (recommendations.size() < limit && !prioritizeSavedAuthors) {
            List<Integer> interestPostIds = new ArrayList<>();
            interestPostIds.addAll(likedIds);
            interestPostIds.addAll(savedIds);

            List<String> categories = List.of();
            if (!interestPostIds.isEmpty()) {
                List<Post> interestPosts = postDao.findVisibleByIds(interestPostIds);
                Map<String, Long> categoryCounts = interestPosts.stream()
                        .map(Post::getCategorie)
                        .filter(value -> value != null && !value.isBlank())
                        .collect(Collectors.groupingBy(value -> value, Collectors.counting()));
                categories = categoryCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .map(Map.Entry::getKey)
                        .limit(3)
                        .toList();
            }

            if (!categories.isEmpty()) {
                recommendations.addAll(postDao.findByCategories(categories, new ArrayList<>(excludeIds), user.id(), limit - recommendations.size()));
                recommendations.forEach(post -> excludeIds.add(post.getId()));
            }
        }

        if (recommendations.size() < limit) {
            List<Integer> currentIds = recommendations.stream().map(Post::getId).toList();
            List<Integer> mergedExclude = new ArrayList<>(excludeIds);
            mergedExclude.addAll(currentIds);
            recommendations.addAll(postDao.findMostPopularRecent(limit - recommendations.size(), mergedExclude, user.id()));
        }

        return recommendations.stream().limit(limit).toList();
    }

    public List<Post> getRelatedToPost(Post post, int limit) throws SQLException {
        if (post == null || limit <= 0) {
            return List.of();
        }
        return postDao.findRelatedToPost(post.getId(), post.getCategorie(), post.getUserId(), limit);
    }
}

