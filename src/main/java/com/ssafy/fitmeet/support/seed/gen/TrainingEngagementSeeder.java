package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoCommentDao;
import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoComment;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainingEngagementSeeder {

    private final Faker faker;
    private final TrainingVideoDao trainingVideoDao;
    private final TrainingVideoCommentDao trainingVideoCommentDao;

    /**
     * 좋아요 시드
     * - perVideoLikeMin~Max: 영상 하나당 좋아요 수 범위
     * - userIds 중에서 랜덤 선택
     */
    @Transactional
    public void seedLikes(
            List<Long> userIds,
            List<Long> videoIds,
            int perVideoLikeMin,
            int perVideoLikeMax
    ) {

        if (userIds == null || userIds.isEmpty()) return;
        if (videoIds == null || videoIds.isEmpty()) return;

        for (Long videoId : videoIds) {

            int likeCnt = faker.number().numberBetween(perVideoLikeMin, perVideoLikeMax + 1);
            likeCnt = Math.min(likeCnt, userIds.size());

            Set<Long> pickedUsers = pickDistinct(userIds, likeCnt);

            for (Long userId : pickedUsers) {
                try {
                    LocalDateTime likedAt = LocalDateTime.now()
                            .minusDays(faker.number().numberBetween(0, 30))
                            .minusMinutes(faker.number().numberBetween(0, 1440));

                    trainingVideoDao.insertLike(userId, videoId, likedAt);
                    trainingVideoDao.increaseLikeCount(videoId);
                } catch (DuplicateKeyException ignore) {}
            }
        }
    }


    /**
     * 댓글 시드
     * - perVideoCommentMin~Max: 영상 하나당 댓글 수 범위
     * - deleteRate: soft delete 비율(0~100)
     */
    @Transactional
    public void seedComments(
            List<Long> userIds,
            List<Long> videoIds,
            int perVideoCommentMin,
            int perVideoCommentMax,
            int deleteRate
    ) {

        if (userIds == null || userIds.isEmpty()) return;
        if (videoIds == null || videoIds.isEmpty()) return;

        for (Long videoId : videoIds) {

            int commentCnt = faker.number().numberBetween(perVideoCommentMin, perVideoCommentMax + 1);

            for (int i = 0; i < commentCnt; i++) {

                Long userId = userIds.get(faker.number().numberBetween(0, userIds.size()));
                String content = faker.lorem().sentence(faker.number().numberBetween(4, 12));

                LocalDateTime commentAt = LocalDateTime.now()
                        .minusDays(faker.number().numberBetween(0, 30))
                        .minusMinutes(faker.number().numberBetween(0, 1440))
                        .plusSeconds(i); // 동일 시각 충돌 방지

                TrainingVideoComment c = TrainingVideoComment.builder()
                        .videoId(videoId)
                        .userId(userId)
                        .content(content)
                        .createdAt(commentAt)
                        .updatedAt(commentAt)
                        .build();

                trainingVideoCommentDao.insertComment(c);

                if (deleteRate > 0 && faker.number().numberBetween(1, 101) <= deleteRate) {
                    trainingVideoCommentDao.softDelete(c.getId());
                }
            }
        }
    }


    /**
     * 조회수 시드
     * - perVideoViewMin~Max: 영상 하나당 조회수 증가 범위
     */
    @Transactional
    public void seedViews(List<Long> videoIds, int perVideoViewMin, int perVideoViewMax) {

        if (videoIds == null || videoIds.isEmpty()) return;

        for (Long videoId : videoIds) {
            long delta = faker.number().numberBetween(perVideoViewMin, perVideoViewMax + 1);
            trainingVideoDao.increaseViewCountBy(videoId, delta);
        }
    }

    private Set<Long> pickDistinct(List<Long> pool, int count) {
        int n = Math.min(count, pool.size());
        Set<Long> picked = new LinkedHashSet<>(n);
        while (picked.size() < n) {
            Long v = pool.get(faker.number().numberBetween(0, pool.size()));
            picked.add(v);
        }
        return picked;
    }
}
