package com.ssafy.fitmeet.support.seed.demo;

import com.ssafy.fitmeet.domain.meal.dao.MealDao;
import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.support.seed.gen.*;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoUserSeeder {

    private final Faker faker;

    private final MealDao mealDao;
    private final TrainingVideoDao trainingVideoDao;

    private final DietSeeder dietSeeder;
    private final DashboardSeeder dashboardSeeder;
    private final TrainingSeeder trainingSeeder;
    private final TrainingEngagementSeeder trainingEngagementSeeder;
    private final SeedFileProvisioner seedFileProvisioner;
    private final CommunitySeeder communitySeeder;

    /**
     * 데모 유저(고정 userId) 전용 시딩
     * - 시연용 “의미 있는 데이터”를 안정적으로 만든다.
     */
    @Transactional
    public void seedDemo(DemoSeedPlan plan, List<Long> allUserIds, List<String> communityImageUrls) {
        Long demoUserId = plan.getDemoUserId();
        if (demoUserId == null) return;

        List<Long> mealIds = mealDao.findAllIds();
        List<Long> videoIds = trainingVideoDao.findAllIds();
        if (mealIds == null || mealIds.isEmpty()) return;
        if (videoIds == null || videoIds.isEmpty()) return;

        List<Long> demoDietIds = dietSeeder.seedDietsForUser(
                demoUserId,
                mealIds,
                plan.getDaysRange(),
                plan.getDietPerDayMin(),
                plan.getDietPerDayMax()
        );

        dashboardSeeder.seedDailyScoreAndActivity(List.of(demoUserId), plan.getDaysRange());

        trainingSeeder.seedWorkoutHistory(
                List.of(demoUserId),
                videoIds,
                plan.getDaysRange(),
                plan.getWorkoutDaysRate(),
                plan.getWorkoutPerDayMin(),
                plan.getWorkoutPerDayMax()
        );

        seedDemoUserLikesOnlySomeVideos(demoUserId, videoIds, plan);

        List<Long> likeCommentPool = (allUserIds == null || allUserIds.isEmpty())
                ? new ArrayList<>(List.of(demoUserId))
                : allUserIds;

        // demo는 이미지 비율 높게(예: 70)
        communitySeeder.seedCommunityForUser(
                demoUserId,
                demoDietIds,
                videoIds,
                plan.getCommunityPosts(),
                plan.getDaysRange(),
                plan.getCommunityLikeMaxPerPost(),
                plan.getCommunityCommentMaxPerPost(),
                plan.getCommunityCommentDeleteRate(),
                likeCommentPool,
                communityImageUrls,
                70
        );
    }


    /**
     * 데모 유저 좋아요는 "영상 N개만" 좋아요 찍히도록 구성
     */
    private void seedDemoUserLikesOnlySomeVideos(Long demoUserId, List<Long> videoIds, DemoSeedPlan plan) {

        int likeCount = faker.number().numberBetween(
                plan.getDemoVideoLikeMin(),
                plan.getDemoVideoLikeMax() + 1
        );

        List<Long> pickedVideoIds = pickDistinct(videoIds, likeCount);

        for (Long videoId : pickedVideoIds) {
            try {
                // created_at: 최근 daysRange 안에서 자연스럽게
                LocalDateTime likedAt = LocalDateTime.now()
                        .minusDays(faker.number().numberBetween(0, Math.max(1, plan.getDaysRange())))
                        .minusMinutes(faker.number().numberBetween(0, 1440));

                trainingVideoDao.insertLike(demoUserId, videoId, likedAt);
                trainingVideoDao.increaseLikeCount(videoId);

            } catch (DuplicateKeyException ignore) {
                // (post_id,user_id) PK 충돌 같은 중복은 무시
            }
        }
    }

    private <T> List<T> pickDistinct(List<T> list, int count) {
        if (list == null || list.isEmpty()) return List.of();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}
