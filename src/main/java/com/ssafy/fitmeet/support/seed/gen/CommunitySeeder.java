package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.community.dao.*;
import com.ssafy.fitmeet.domain.community.entity.CommunityComment;
import com.ssafy.fitmeet.domain.community.entity.CommunityPost;
import com.ssafy.fitmeet.domain.community.entity.CommunityTag;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommunitySeeder {

    private final Faker faker;

    private final CommunityPostDao postDao;
    private final CommunityPostDietDao postDietDao;
    private final CommunityPostVideoDao postVideoDao;
    private final CommunityPostTagDao postTagDao;
    private final CommunityTagDao tagDao;
    private final CommunityLikeDao likeDao;
    private final CommunityCommentDao commentDao;

    /**
     * Community 전체 시딩
     */
    @Transactional
    public void seedCommunity(
            List<Long> userIds,
            List<Long> dietIds,
            List<Long> videoIds,
            int postCount,
            int daysRange,
            int likeMaxPerPost,
            int commentMaxPerPost,
            int commentDeleteRate,
            List<String> imageUrls,
            int imageRate
    ) {
        if (userIds == null || userIds.isEmpty()) return;

        List<String> tagPool = List.of(
                "식단", "운동", "다이어트", "헬스", "홈트",
                "건강", "스트릭", "루틴", "도전", "기록"
        );

        for (int i = 0; i < postCount; i++) {

            Long authorId = pickOne(userIds);
            LocalDateTime createdAt = randomDateTime(daysRange);
            String imageUrl = pickImage(imageUrls, imageRate);

            CommunityPost post = CommunityPost.builder()
                    .userId(authorId)
                    .title(faker.lorem().sentence(4))
                    .content(faker.lorem().paragraph(3))
                    .category(pickOne(List.of("GENERAL", "DIET", "WORKOUT")))
                    .postImageUrl(imageUrl)
                    .activityDate(createdAt.toLocalDate())
                    .shareDiet(faker.bool().bool())
                    .shareStreak(faker.bool().bool())
                    .shareWorkout(faker.bool().bool())
                    .dietNote(faker.bool().bool() ? faker.lorem().sentence(5) : null)
                    .workoutNote(faker.bool().bool() ? faker.lorem().sentence(5) : null)
                    .likeCount(0)
                    .viewCount(faker.number().numberBetween(0, 300))
                    .commentCount(0)
                    .isDeleted(false)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();

            postDao.insert(post);
            Long postId = post.getId();

            /* ================= Diet 공유 ================= */
            if (post.isShareDiet() && dietIds != null && !dietIds.isEmpty()) {
                int cnt = faker.number().numberBetween(1, Math.min(3, dietIds.size()) + 1);
                for (Long dietId : pickDistinct(dietIds, cnt)) {
                    postDietDao.insert(postId, dietId);
                }
            }

            /* ================= Workout Video 공유 ================= */
            if (post.isShareWorkout() && videoIds != null && !videoIds.isEmpty()) {
                int cnt = faker.number().numberBetween(1, Math.min(3, videoIds.size()) + 1);
                for (Long videoId : pickDistinct(videoIds, cnt)) {
                    postVideoDao.insert(postId, videoId);
                }
            }

            /* ================= Tags ================= */
            int tagCnt = faker.number().numberBetween(1, 4);
            for (String tagName : pickDistinct(tagPool, tagCnt)) {
                CommunityTag tag = tagDao.findByName(tagName);
                if (tag == null) {
                    tag = CommunityTag.builder()
                            .name(tagName)
                            .usageCount(0)
                            .createdAt(createdAt)
                            .updatedAt(createdAt)
                            .build();
                    tagDao.insert(tag);
                }
                postTagDao.insert(postId, tag.getId());
                tagDao.increaseUsageCount(tag.getId(), 1);
            }

            /* ================= Likes (createdAt 일관 반영) ================= */
            int likeCnt = faker.number().numberBetween(0, Math.min(likeMaxPerPost, userIds.size()) + 1);
            for (Long likerId : pickDistinct(userIds, likeCnt)) {

                if (likeDao.exists(postId, likerId) > 0) continue;

                // 좋아요는 글 생성 이후 ~ 다음날 사이 임의 시각으로 찍히게 (자연스러움)
                LocalDateTime likeAt = createdAt.plusMinutes(
                        faker.number().numberBetween(1, 24 * 60) // 1~1440분
                );

                likeDao.insert(postId, likerId, likeAt);
                postDao.updateLikeCount(postId, +1);
            }

            /* ================= Comments ================= */
            int commentCnt = faker.number().numberBetween(0, commentMaxPerPost + 1);
            for (int c = 0; c < commentCnt; c++) {

                Long commenterId = pickOne(userIds);

                // 댓글은 글 생성 후 1~8시간 내 분산
                LocalDateTime commentAt = createdAt.plusMinutes(
                        faker.number().numberBetween(1, 8 * 60)
                ).plusSeconds(c); // 동일 분 충돌 방지용

                CommunityComment comment = CommunityComment.builder()
                        .postId(postId)
                        .userId(commenterId)
                        .content(faker.lorem().sentence(8))
                        .parentId(null)
                        .isDeleted(false)
                        .createdAt(commentAt)
                        .updatedAt(commentAt)
                        .build();

                commentDao.insert(comment);
                postDao.updateCommentCount(postId, +1);

                // 일부 댓글 soft delete
                if (faker.number().numberBetween(0, 100) < commentDeleteRate) {
                    commentDao.softDelete(comment.getId(), commenterId);
                    postDao.updateCommentCount(postId, -1);
                }
            }
        }
    }

    /**
     *  데모 유저가 작성자(authorUserId)인 게시글을 생성하고,
     *    좋아요/댓글은 likeCommentPool(=seed 유저들)이 달게 한다.
     */
    @Transactional
    public void seedCommunityForUser(
            Long authorUserId,
            List<Long> dietIdsForAuthor, // demo 유저 dietIds (공유용)
            List<Long> videoIds, // 공유용
            int postCount,
            int daysRange,
            int likeMaxPerPost,
            int commentMaxPerPost,
            int commentDeleteRate,
            List<Long> likeCommentPool,
            List<String> imageUrls,
            int imageRate// 좋아요/댓글 참여자 풀(전체 유저)
    ) {
        if (authorUserId == null) return;

        List<Long> pool = (likeCommentPool == null || likeCommentPool.isEmpty())
                ? List.of(authorUserId)
                : likeCommentPool;

        List<String> tagPool = List.of(
                "식단", "운동", "다이어트", "헬스", "홈트",
                "건강", "스트릭", "루틴", "도전", "기록"
        );

        for (int i = 0; i < postCount; i++) {

            LocalDateTime createdAt = randomDateTime(daysRange);
            String imageUrl = pickImage(imageUrls, imageRate);
            CommunityPost post = CommunityPost.builder()
                    .userId(authorUserId)
                    .title(faker.lorem().sentence(4))
                    .content(faker.lorem().paragraph(3))
                    .category(pickOne(List.of("GENERAL", "DIET", "WORKOUT")))
                    .postImageUrl(imageUrl)
                    .activityDate(createdAt.toLocalDate())
                    .shareDiet(faker.bool().bool())
                    .shareStreak(faker.bool().bool())
                    .shareWorkout(faker.bool().bool())
                    .dietNote(faker.bool().bool() ? faker.lorem().sentence(5) : null)
                    .workoutNote(faker.bool().bool() ? faker.lorem().sentence(5) : null)
                    .likeCount(0)
                    .viewCount(faker.number().numberBetween(0, 300))
                    .commentCount(0)
                    .isDeleted(false)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();

            postDao.insert(post);
            Long postId = post.getId();

            // ================= Diet 공유 (demo dietIds에서만 뽑기) =================
            if (post.isShareDiet() && dietIdsForAuthor != null && !dietIdsForAuthor.isEmpty()) {
                int cnt = faker.number().numberBetween(1, Math.min(3, dietIdsForAuthor.size()) + 1);
                for (Long dietId : pickDistinct(dietIdsForAuthor, cnt)) {
                    postDietDao.insert(postId, dietId);
                }
            }

            // ================= Workout Video 공유 =================
            if (post.isShareWorkout() && videoIds != null && !videoIds.isEmpty()) {
                int cnt = faker.number().numberBetween(1, Math.min(3, videoIds.size()) + 1);
                for (Long videoId : pickDistinct(videoIds, cnt)) {
                    postVideoDao.insert(postId, videoId);
                }
            }

            // ================= Tags =================
            int tagCnt = faker.number().numberBetween(1, 4);
            for (String tagName : pickDistinct(tagPool, tagCnt)) {
                CommunityTag tag = tagDao.findByName(tagName);
                if (tag == null) {
                    tag = CommunityTag.builder()
                            .name(tagName)
                            .usageCount(0)
                            .createdAt(createdAt)
                            .updatedAt(createdAt)
                            .build();
                    tagDao.insert(tag);
                }
                postTagDao.insert(postId, tag.getId());
                tagDao.increaseUsageCount(tag.getId(), 1);
            }

            // ================= Likes: pool 유저들이 참여 =================
            int likeCnt = faker.number().numberBetween(0, Math.min(likeMaxPerPost, pool.size()) + 1);
            for (Long likerId : pickDistinct(pool, likeCnt)) {
                // 원하면 작성자 제외: if (Objects.equals(likerId, authorUserId)) continue;

                if (likeDao.exists(postId, likerId) > 0) continue;

                LocalDateTime likeAt = createdAt.plusMinutes(
                        faker.number().numberBetween(1, 24 * 60)
                );

                likeDao.insert(postId, likerId, likeAt);
                postDao.updateLikeCount(postId, +1);
            }

            // ================= Comments: pool 유저들이 참여 =================
            int commentCnt = faker.number().numberBetween(0, commentMaxPerPost + 1);
            for (int c = 0; c < commentCnt; c++) {

                Long commenterId = pickOne(pool);
                // 원하면 작성자 제외:
                // if (Objects.equals(commenterId, authorUserId)) commenterId = pickOne(pool);

                LocalDateTime commentAt = createdAt.plusMinutes(
                        faker.number().numberBetween(1, 8 * 60)
                ).plusSeconds(c);

                CommunityComment comment = CommunityComment.builder()
                        .postId(postId)
                        .userId(commenterId)
                        .content(faker.lorem().sentence(8))
                        .parentId(null)
                        .isDeleted(false)
                        .createdAt(commentAt)
                        .updatedAt(commentAt)
                        .build();

                commentDao.insert(comment);
                postDao.updateCommentCount(postId, +1);

                if (faker.number().numberBetween(0, 100) < commentDeleteRate) {
                    commentDao.softDelete(comment.getId(), commenterId);
                    postDao.updateCommentCount(postId, -1);
                }
            }
        }
    }


    /* ================= helpers ================= */

    private <T> T pickOne(List<T> list) {
        return list.get(faker.number().numberBetween(0, list.size()));
    }

    private <T> List<T> pickDistinct(List<T> list, int count) {
        if (list == null || list.isEmpty()) return List.of();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private LocalDateTime randomDateTime(int daysRange) {
        LocalDate date = LocalDate.now()
                .minusDays(faker.number().numberBetween(0, daysRange + 1));

        int hour = faker.number().numberBetween(7, 23);
        int minute = faker.number().numberBetween(0, 60);

        return date.atTime(hour, minute);
    }

    private String pickImage(List<String> imageUrls, int imageRate) {
        if (imageUrls == null || imageUrls.isEmpty()) return null;
        if (faker.number().numberBetween(0, 100) < imageRate) {
            return pickOne(imageUrls);
        }
        return null;
    }

}
