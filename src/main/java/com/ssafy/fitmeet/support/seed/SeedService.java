package com.ssafy.fitmeet.support.seed;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.support.seed.dao.SeedHistoryDao;
import com.ssafy.fitmeet.support.seed.demo.DemoSeedPlan;
import com.ssafy.fitmeet.support.seed.demo.DemoUserSeeder;
import com.ssafy.fitmeet.support.seed.demo.SeedFileProvisioner;
import com.ssafy.fitmeet.support.seed.gen.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    private final DemoUserSeeder demoUserSeeder;
    private final SeedFileProvisioner seedFileProvisioner;

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
            return;
        }

        // 3) 파일 provision (락 이후)
        List<String> communityImages = seedFileProvisioner.provisionCommunityImages("seed-images/community-post");
        List<String> profileImages = seedFileProvisioner.provisionProfileImages("seed-images/profile");

        // 4) User
        List<Long> userIds = userSeeder.seedUsers(plan.getUsers());
        userSeeder.seedBodyInfos(userIds, plan.getBodyInfoRate());

        // 4-1) 프로필 사진: 일반 유저도 넣기 (예: 60%)
        userSeeder.seedProfileImages(userIds, profileImages, 60);

        // 5) Meal
        List<Long> mealIds = mealSeeder.seedMeals(plan.getMeals());
        mealSeeder.seedMealNutrition(mealIds);

        // 6) Diet
        List<Long> dietIds = dietSeeder.seedDiets(userIds, mealIds, plan.getDiets(), plan.getDietDaysRange());

        // 7) Dashboard
        dashboardSeeder.seedDailyScoreAndActivity(userIds, plan.getDashboardDaysRange());

        // 8) AI weekly logs
        dietAiLogSeeder.seedWeeklyAiLogs(userIds, plan.getAiWeeksBack(), plan.getAiLogsPerUserMax());

        // 9) Training
        List<Long> videoIds = trainingVideoDao.findAllIds();
        trainingSeeder.seedWorkoutHistory(
                userIds,
                videoIds,
                plan.getWorkoutDaysRange(),
                plan.getWorkoutDaysRate(),
                plan.getWorkoutPerDayMin(),
                plan.getWorkoutPerDayMax()
        );

        // 10) Video engagement
        trainingEngagementSeeder.seedViews(videoIds, plan.getPerVideoViewMin(), plan.getPerVideoViewMax());
        trainingEngagementSeeder.seedLikes(userIds, videoIds, plan.getPerVideoLikeMin(), plan.getPerVideoLikeMax());
        trainingEngagementSeeder.seedComments(userIds, videoIds, plan.getPerVideoCommentMin(), plan.getPerVideoCommentMax(), plan.getVideoCommentDeleteRate());

        // 11) Community: 모든 유저 글에도 이미지 일부 포함 (예: 30%)
        communitySeeder.seedCommunity(
                userIds,
                dietIds,
                videoIds,
                plan.getCommunityPosts(),
                plan.getCommunityDaysRange(),
                plan.getCommunityLikeMaxPerPost(),
                plan.getCommunityCommentMaxPerPost(),
                plan.getCommunityCommentDeleteRate(),
                communityImages,
                30
        );

        // 12) Demo: 미리 만들어둔 데모 유저 기준으로 시딩
        DemoSeedPlan demoPlan = DemoSeedPlan.defaultPlan();
        Long demoUserId = demoPlan.getDemoUserId(); // ← 여기 중요

        if (demoUserId == null) {
            return;
        }

        // 데모 유저는 프로필 사진 100%
        userSeeder.seedProfileImageForUser(demoUserId, profileImages);

        // like/comment pool: 데모 + 일반 시드 유저
        LinkedHashSet<Long> poolSet = new LinkedHashSet<>();
        poolSet.add(demoUserId);
        poolSet.addAll(userIds);
        List<Long> likeCommentPool = new ArrayList<>(poolSet);

        // 데모 전용 시딩 실행
        demoUserSeeder.seedDemo(demoPlan, likeCommentPool, communityImages);

    }
}
