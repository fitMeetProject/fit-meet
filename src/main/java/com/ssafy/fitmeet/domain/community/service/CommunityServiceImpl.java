package com.ssafy.fitmeet.domain.community.service;

import com.ssafy.fitmeet.domain.community.dao.*;
import com.ssafy.fitmeet.domain.community.dto.CommunityDto.*;
import com.ssafy.fitmeet.domain.community.entity.CommunityComment;
import com.ssafy.fitmeet.domain.community.entity.CommunityPost;
import com.ssafy.fitmeet.domain.community.entity.CommunityTag;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.diet.service.UserDailyNutritionService;
import com.ssafy.fitmeet.domain.user.dto.ActivityDto.DietActivityDay;
import com.ssafy.fitmeet.domain.user.dto.ActivityDto.DietStreakInfo;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.domain.user.service.UserDailyActivityService;
import com.ssafy.fitmeet.domain.user.service.UserService;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostDao postDao;
    private final CommunityTagDao tagDao;
    private final CommunityPostTagDao postTagDao;
    private final CommunityLikeDao likeDao;
    private final CommunityCommentDao commentDao;
    private final UserService userService;
    private final UserDailyActivityService userDailyActivityService;
    private final UserDailyNutritionService userDailyNutritionService;

    private int normalizePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        return (size == null || size < 1) ? 20 : size;
    }

    @Override
    @Transactional
    public Long createPost(Long userId, PostCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();

        CommunityPost post = CommunityPost.builder()
                .userId(userId)
                .title(request.title())
                .content(request.content())
                .category(request.category() != null ? request.category() : "GENERAL")
                .activityDate(request.activityDate())
                .shareDiet(Boolean.TRUE.equals(request.shareDiet()))
                .shareStreak(Boolean.TRUE.equals(request.shareStreak()))
                .shareWorkout(Boolean.TRUE.equals(request.shareWorkout()))
                .workoutNote(request.workoutNote())
                .workoutVideoId(request.workoutVideoId())
                .workoutVideoUrl(request.workoutVideoUrl())
                .likeCount(0)
                .viewCount(0)
                .commentCount(0)
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        postDao.insert(post);

        // 태그 처리
        List<String> tags = normalizeTags(request.tags());
        for (String tagName : tags) {
            CommunityTag tag = tagDao.findByName(tagName);
            if (tag == null) {
                tag = CommunityTag.builder()
                        .name(tagName)
                        .usageCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                tagDao.insert(tag);
            }
            postTagDao.insert(post.getId(), tag.getId());
            tagDao.increaseUsageCount(tag.getId(), 1);
        }

        return post.getId();
    }

    @Override
    @Transactional
    public PostDetailResponse getPostDetail(Long currentUserId, Long postId, boolean isCreate) {
        if(!isCreate) postDao.increaseViewCount(postId);

        CommunityPost post = postDao.findById(postId);
        if (post == null || post.isDeleted()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        boolean likedByMe = false;
        if (currentUserId != null) {
            Boolean exists = likeDao.exists(postId, currentUserId);
            likedByMe = Boolean.TRUE.equals(exists);
        }

        User writer = userService.getUserById(post.getUserId());
        List<String> tags = postTagDao.findTagsByPostId(postId);

        ActivitySummaryResponse activitySummary = buildActivitySummary(post);

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory(),
                post.getActivityDate(),
                post.isShareDiet(),
                post.isShareStreak(),
                post.isShareWorkout(),
                post.getLikeCount(),
                isCreate?post.getViewCount() : post.getViewCount() + 1,
                post.getCommentCount(),
                likedByMe,
                tags,
                writer.getNickname(),
                writer.getProfileImageUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                activitySummary
        );
    }

    private ActivitySummaryResponse buildActivitySummary(CommunityPost post) {
        if (post.getActivityDate() == null ||
                (!post.isShareDiet() && !post.isShareStreak() && !post.isShareWorkout())) {
            return null;
        }

        LocalDate targetDate = post.getActivityDate();
        Long ownerId = post.getUserId();

        boolean dietLogged = false;
        boolean workoutLogged = false;
        boolean challengeDone = false;

        List<DietActivityDay> days = userDailyActivityService.getDietActivityRange(ownerId, targetDate, targetDate);
        if (!days.isEmpty()) {
            DietActivityDay d = days.get(0);
            dietLogged = d.dietLogged();
        }

        Double todayKcal = null;
        Double todayCarbo = null;
        Double todayProtein = null;
        Double todayFat = null;
        Double todaySugar = null;
        Double todaySodium = null;

        if (post.isShareDiet()) {
            UserDailyNutrition nut = userDailyNutritionService.getDaily(ownerId, targetDate);
            if (nut != null) {
                todayKcal = nut.getTotalKcal() != null ? nut.getTotalKcal().doubleValue() : 0.0;
                todayCarbo = nut.getTotalCarbohydrate() != null ? nut.getTotalCarbohydrate().doubleValue() : 0.0;
                todayProtein = nut.getTotalProtein() != null ? nut.getTotalProtein().doubleValue() : 0.0;
                todayFat = nut.getTotalFat() != null ? nut.getTotalFat().doubleValue() : 0.0;
                todaySugar = nut.getTotalSugar() != null ? nut.getTotalSugar().doubleValue() : 0.0;
                todaySodium = nut.getTotalSodium() != null ? nut.getTotalSodium().doubleValue() : 0.0;
            }
        }

        Integer streakCount = null;
        if (post.isShareStreak()) {
            DietStreakInfo streakInfo = userDailyActivityService.getCurrentDietStreak(ownerId);
            streakCount = streakInfo.streakCount();
        }

        return new ActivitySummaryResponse(
                targetDate,
                dietLogged,
                workoutLogged,
                challengeDone,
                todayKcal,
                todayCarbo,
                todayProtein,
                todayFat,
                todaySugar,
                todaySodium,
                streakCount,
                post.isShareWorkout() ? post.getWorkoutNote() : null,
                post.isShareWorkout() ? post.getWorkoutVideoId() : 0,
                post.isShareWorkout() ? post.getWorkoutVideoUrl() : null
        );
    }

    @Override
    @Transactional
    public void updatePost(Long userId, Long postId, PostUpdateRequest request) {
        CommunityPost post = postDao.findById(postId);
        if (post == null || post.isDeleted()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        post.setTitle(request.title());
        post.setContent(request.content());
        post.setCategory(request.category() != null ? request.category() : post.getCategory());
        post.setActivityDate(request.activityDate());
        post.setShareDiet(Boolean.TRUE.equals(request.shareDiet()));
        post.setShareStreak(Boolean.TRUE.equals(request.shareStreak()));
        post.setShareWorkout(Boolean.TRUE.equals(request.shareWorkout()));
        post.setWorkoutNote(request.workoutNote());
        post.setWorkoutVideoId(request.workoutVideoId());
        post.setWorkoutVideoUrl(request.workoutVideoUrl());
        post.setUpdatedAt(LocalDateTime.now());
        postDao.update(post);

        // 태그 재설정
        List<String> oldTags = postTagDao.findTagsByPostId(postId);
        postTagDao.deleteByPostId(postId);
        for (String tagName : oldTags) {
            CommunityTag tag = tagDao.findByName(tagName);
            if (tag != null) {
                tagDao.increaseUsageCount(tag.getId(), -1);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> newTags = normalizeTags(request.tags());
        for (String tagName : newTags) {
            CommunityTag tag = tagDao.findByName(tagName);
            if (tag == null) {
                tag = CommunityTag.builder()
                        .name(tagName)
                        .usageCount(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                tagDao.insert(tag);
            }
            postTagDao.insert(postId, tag.getId());
            tagDao.increaseUsageCount(tag.getId(), 1);
        }
    }

    @Override
    @Transactional
    public void softDeletePost(Long userId, Long postId) {
        int updated = postDao.softDelete(postId, userId);
        if (updated == 0) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    @Override
    @Transactional
    public void restorePost(Long userId, Long postId) {
        int updated = postDao.restore(postId, userId);
        if (updated == 0) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        CommunityPost post = postDao.findById(postId);
        if (post == null || post.isDeleted()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        Boolean exists = likeDao.exists(postId, userId);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        likeDao.insert(postId, userId);
        postDao.updateLikeCount(postId, +1);
    }

    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {
        Boolean exists = likeDao.exists(postId, userId);
        if (!Boolean.TRUE.equals(exists)) {
            return;
        }

        likeDao.delete(postId, userId);
        postDao.updateLikeCount(postId, -1);
    }

    @Override
    @Transactional
    public Long addComment(Long userId, Long postId, CommentCreateRequest request) {
        CommunityPost post = postDao.findById(postId);
        if (post == null || post.isDeleted()) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();

        CommunityComment comment = CommunityComment.builder()
                .postId(postId)
                .userId(userId)
                .content(request.content())
                .parentId(request.parentId())
                .isDeleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        commentDao.insert(comment);
        postDao.updateCommentCount(postId, +1);

        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        CommunityComment comment = commentDao.findById(commentId);
        if (comment == null || comment.isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        int updated = commentDao.softDelete(commentId, userId);
        if (updated == 0) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        postDao.updateCommentCount(comment.getPostId(), -1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long currentUserId, Long postId) {
        List<CommunityComment> comments = commentDao.findByPostId(postId);
        List<CommentResponse> result = new ArrayList<>();

        for (CommunityComment c : comments) {
            User writer = userService.getUserById(c.getUserId());
            String content = c.isDeleted() ? "삭제된 댓글입니다." : c.getContent();

            result.add(new CommentResponse(
                    c.getId(),
                    c.getPostId(),
                    content,
                    c.isDeleted(),
                    c.getParentId(),
                    writer.getNickname(),
                    writer.getProfileImageUrl(),
                    c.getCreatedAt()
            ));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PostPageResponse searchPosts(Long currentUserId, PostSearchCondition cond) {
        int page = normalizePage(cond.page());
        int size = normalizeSize(cond.size());
        int offset = page * size;

        PostSearchCondition fixed = new PostSearchCondition(
                cond.keyword(),
                cond.tag(),
                cond.category(),
                cond.sort(),
                page, size, offset
        );

        long totalElements = postDao.countSearch(fixed);
        List<CommunityPost> posts = postDao.search(fixed);

        List<PostSummaryResponse> content = new ArrayList<>();
        for (CommunityPost p : posts) {
            boolean likedByMe = false;
            if (currentUserId != null) {
                likedByMe = Boolean.TRUE.equals(likeDao.exists(p.getId(), currentUserId));
            }

            User writer = userService.getUserById(p.getUserId());
            List<String> tags = postTagDao.findTagsByPostId(p.getId());

            String preview = p.getContent();
            if (preview != null && preview.length() > 100) preview = preview.substring(0, 100) + "...";

            content.add(new PostSummaryResponse(
                    p.getId(),
                    p.getTitle(),
                    preview,
                    p.getCategory(),
                    p.getActivityDate(),
                    p.isShareDiet(),
                    p.isShareStreak(),
                    p.isShareWorkout(),
                    p.getLikeCount(),
                    p.getViewCount(),
                    p.getCommentCount(),
                    likedByMe,
                    tags,
                    writer.getNickname(),
                    writer.getProfileImageUrl(),
                    p.getCreatedAt()
            ));
        }

        return PostPageResponse.of(content, page, size, totalElements);
    }


    @Override
    @Transactional(readOnly = true)
    public PostPageResponse getMyLikedPosts(Long userId, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = p * s;

        long totalElements = postDao.countLikedPostsByUser(userId);
        List<CommunityPost> posts = postDao.findLikedPostsByUserPaged(userId, s, offset);

        List<PostSummaryResponse> content = new ArrayList<>();
        for (CommunityPost post : posts) {
            User writer = userService.getUserById(post.getUserId());
            List<String> tags = postTagDao.findTagsByPostId(post.getId());

            String preview = post.getContent();
            if (preview != null && preview.length() > 100) preview = preview.substring(0, 100) + "...";

            content.add(new PostSummaryResponse(
                    post.getId(),
                    post.getTitle(),
                    preview,
                    post.getCategory(),
                    post.getActivityDate(),
                    post.isShareDiet(),
                    post.isShareStreak(),
                    post.isShareWorkout(),
                    post.getLikeCount(),
                    post.getViewCount(),
                    post.getCommentCount(),
                    true,
                    tags,
                    writer.getNickname(),
                    writer.getProfileImageUrl(),
                    post.getCreatedAt()
            ));
        }

        return PostPageResponse.of(content, p, s, totalElements);
    }


    @Override
    @Transactional(readOnly = true)
    public PostPageResponse getMyPosts(Long userId, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = p * s;

        long totalElements = postDao.countByUserId(userId);
        List<CommunityPost> posts = postDao.findByUserIdPaged(userId, s, offset);

        List<PostSummaryResponse> content = new ArrayList<>();
        for (CommunityPost post : posts) {
            boolean likedByMe = Boolean.TRUE.equals(likeDao.exists(post.getId(), userId));

            User writer = userService.getUserById(post.getUserId());
            List<String> tags = postTagDao.findTagsByPostId(post.getId());

            String preview = post.getContent();
            if (preview != null && preview.length() > 100) preview = preview.substring(0, 100) + "...";

            content.add(new PostSummaryResponse(
                    post.getId(),
                    post.getTitle(),
                    preview,
                    post.getCategory(),
                    post.getActivityDate(),
                    post.isShareDiet(),
                    post.isShareStreak(),
                    post.isShareWorkout(),
                    post.getLikeCount(),
                    post.getViewCount(),
                    post.getCommentCount(),
                    likedByMe,
                    tags,
                    writer.getNickname(),
                    writer.getProfileImageUrl(),
                    post.getCreatedAt()
            ));
        }

        return PostPageResponse.of(content, p, s, totalElements);
    }



    @Override
    @Transactional(readOnly = true)
    public List<TagSummaryResponse> getTopTags(int limit) {
        return tagDao.findTopTags(limit).stream()
                .map(t -> new TagSummaryResponse(t.getId(), t.getName(), t.getUsageCount()))
                .toList();
    }

    private List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null) return List.of();
        List<String> list = new ArrayList<>();
        for (String t : rawTags) {
            if (t == null) continue;
            String s = t.trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }
}
