package com.ssafy.fitmeet.support.seed;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.support.seed.dao.SeedHistoryDao;
import com.ssafy.fitmeet.support.seed.gen.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedService {

    private final SeedHistoryDao seedHistoryDao;

    private final UserSeeder userSeeder;
    private final MealSeeder mealSeeder;
    private final DietSeeder dietSeeder;
    private final DashboardSeeder dashboardSeeder;
    private final DietAiLogSeeder dietAiLogSeeder;

    private final TrainingVideoDao trainingVideoDao;
    private final TrainingSeeder trainingSeeder;
    private final TrainingEngagementSeeder trainingEngagementSeeder;

    private final CommunitySeeder communitySeeder;

    @Value("${app.seed.secret}")
    private String expectedSecret;

    private static final String SEED_NAME = "fitmeet_seed_v1";

    @Transactional
    public void runOnce(String secret, SeedPlan plan) {

        // 1) 시크릿 체크
        if (expectedSecret == null || expectedSecret.isBlank() || !expectedSecret.equals(secret)) {
            throw new IllegalArgumentException("Forbidden: bad seed secret");
        }

        // 2) 1회 실행 락
        try {
            seedHistoryDao.insert(SEED_NAME);
        } catch (DuplicateKeyException e) {
            return; // 이미 실행됨
        }

        // 3) FK 순서: User -> BodyInfo -> Meal -> MealNutrition -> Diet -> Dashboard -> AI -> Training -> Community
        List<Long> userIds = userSeeder.seedUsers(plan.getUsers());
        userSeeder.seedBodyInfos(userIds, plan.getBodyInfoRate());

        List<Long> mealIds = mealSeeder.seedMeals(plan.getMeals());
        mealSeeder.seedMealNutrition(mealIds);

        // Diet (dietIds 리턴 받으면 Community에서 활용 가능)
        List<Long> dietIds = dietSeeder.seedDiets(userIds, mealIds, plan.getDiets(), plan.getDietDaysRange());

        // Dashboard
        dashboardSeeder.seedDailyScoreAndActivity(userIds, plan.getDashboardDaysRange());

        // AI weekly logs
        dietAiLogSeeder.seedWeeklyAiLogs(userIds, plan.getAiWeeksBack(), plan.getAiLogsPerUserMax());

        // Training
        List<Long> videoIds = trainingVideoDao.findAllIds();
        trainingSeeder.seedWorkoutHistory(
                userIds,
                videoIds,
                plan.getWorkoutDaysRange(),
                plan.getWorkoutDaysRate(),
                plan.getWorkoutPerDayMin(),
                plan.getWorkoutPerDayMax()
        );

        // Video engagement (views/likes/comments)
        trainingEngagementSeeder.seedViews(videoIds, plan.getPerVideoViewMin(), plan.getPerVideoViewMax());
        trainingEngagementSeeder.seedLikes(userIds, videoIds, plan.getPerVideoLikeMin(), plan.getPerVideoLikeMax());
        trainingEngagementSeeder.seedComments(userIds, videoIds, plan.getPerVideoCommentMin(), plan.getPerVideoCommentMax(), plan.getVideoCommentDeleteRate());

        // Community (Diet/Video 공유를 위해 dietIds/videoIds 필요)
        communitySeeder.seedCommunity(
                userIds,
                dietIds,
                videoIds,
                plan.getCommunityPosts(),
                plan.getCommunityDaysRange(),
                plan.getCommunityLikeMaxPerPost(),
                plan.getCommunityCommentMaxPerPost(),
                plan.getCommunityCommentDeleteRate()
        );
    }
}
