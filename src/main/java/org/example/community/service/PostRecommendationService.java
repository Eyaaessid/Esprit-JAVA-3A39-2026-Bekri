package org.example.community.service;

import org.example.community.dao.LikeDao;
import org.example.community.dao.PostDao;
import org.example.community.dao.SavedPostDao;
import org.example.community.model.Post;
import org.example.community.model.UserSummary;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        if (limit <= 0) {
            return List.of();
        }

        if (user == null) {
            return postDao.findMostPopularRecent(limit, excludedFeedIds, null);
        }

        Set<Integer> excludeIds = new LinkedHashSet<>(excludedFeedIds == null ? List.of() : excludedFeedIds);
        excludeIds.addAll(likeDao.findLikedPostIdsByUser(user.id()));
        excludeIds.addAll(savedPostDao.findSavedPostIdsByUser(user.id()));
        excludeIds.addAll(postDao.findPostIdsByAuthor(user.id()));

        List<Post> recommendations = new ArrayList<>();
        List<String> categories = postDao.findDistinctCategoriesByAuthor(user.id());
        if (!categories.isEmpty()) {
            recommendations.addAll(postDao.findByCategories(categories, new ArrayList<>(excludeIds), user.id(), limit));
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
