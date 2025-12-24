package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.community.dao.*;
import com.ssafy.fitmeet.domain.community.entity.*;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Profile({"local", "dev"})
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
            int commentDeleteRate
    ) {

        if (userIds.isEmpty()) return;

        List<String> tagPool = List.of(
                "식단", "운동", "다이어트", "헬스", "홈트",
                "건강", "스트릭", "루틴", "도전", "기록"
        );

        for (int i = 0; i < postCount; i++) {

            Long authorId = pickOne(userIds);
            LocalDateTime createdAt = randomDateTime(daysRange);

            CommunityPost post = CommunityPost.builder()
                    .userId(authorId)
                    .title(faker.lorem().sentence(4))
                    .content(faker.lorem().paragraph(3))
                    .category(pickOne(List.of("GENERAL", "DIET", "WORKOUT")))
                    .postImageUrl(null)
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
            if (post.isShareDiet() && !dietIds.isEmpty()) {
                int cnt = faker.number().numberBetween(1, Math.min(3, dietIds.size()) + 1);
                for (Long dietId : pickDistinct(dietIds, cnt)) {
                    postDietDao.insert(postId, dietId);
                }
            }

            /* ================= Workout Video 공유 ================= */
            if (post.isShareWorkout() && !videoIds.isEmpty()) {
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

            /* ================= Likes ================= */
            int likeCnt = faker.number().numberBetween(0, Math.min(likeMaxPerPost, userIds.size()) + 1);
            for (Long likerId : pickDistinct(userIds, likeCnt)) {
                if (likeDao.exists(postId, likerId) == 0) {
                    likeDao.insert(postId, likerId);
                    postDao.updateLikeCount(postId, +1);
                }
            }

            /* ================= Comments ================= */
            int commentCnt = faker.number().numberBetween(0, commentMaxPerPost + 1);
            for (int c = 0; c < commentCnt; c++) {

                Long commenterId = pickOne(userIds);
                LocalDateTime commentAt = createdAt.plusMinutes(c + 1);

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

    /* ================= helpers ================= */

    private <T> T pickOne(List<T> list) {
        return list.get(faker.number().numberBetween(0, list.size()));
    }

    private <T> List<T> pickDistinct(List<T> list, int count) {
        if (list.isEmpty()) return List.of();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private LocalDateTime randomDateTime(int daysRange) {
        LocalDate date = LocalDate.now()
                .minusDays(faker.number().numberBetween(0, daysRange + 1));
        return date.atTime(
                faker.number().numberBetween(7, 23),
                faker.number().numberBetween(0, 60)
        );
    }
}
