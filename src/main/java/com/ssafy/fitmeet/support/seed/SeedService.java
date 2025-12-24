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
    private final DietAiLogSeeder dietAiLogSeeder;
    private final DashboardSeeder dashboardSeeder;
    private final TrainingVideoDao trainingVideoDao;
    private final TrainingSeeder trainingSeeder;

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
            // 이미 실행됨 -> 그대로 종료(데이터 유지 목적)
            return;
        }

        // 3) FK 순서: User -> BodyInfo(선택) -> Meal -> MealNutrition
        List<Long> userIds = userSeeder.seedUsers(plan.getUsers());
        userSeeder.seedBodyInfos(userIds, plan.getBodyInfoRate());

        List<Long> mealIds = mealSeeder.seedMeals(plan.getMeals());
        mealSeeder.seedMealNutrition(mealIds);

        dietSeeder.seedDiets(userIds, mealIds, plan.getDiets(), plan.getDietDaysRange());

        // 대시보드
        dashboardSeeder.seedDailyScoreAndActivity(userIds, plan.getDashboardDaysRange());

        // AI 주간 로그
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

    }
}
