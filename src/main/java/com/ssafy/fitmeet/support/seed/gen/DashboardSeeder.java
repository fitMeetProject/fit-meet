package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.dashboard.dao.UserDailyScoreDao;
import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import com.ssafy.fitmeet.domain.diet.dao.UserDailyNutritionDao;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.user.dao.UserBodyInfoDao;
import com.ssafy.fitmeet.domain.user.dao.UserDailyActivityDao;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.entity.UserDailyActivity;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardSeeder {

    private final Faker faker;

    private final UserDailyScoreDao userDailyScoreDao;
    private final UserDailyNutritionDao userDailyNutritionDao;

    private final UserBodyInfoDao userBodyInfoDao;
    private final UserDailyActivityDao userDailyActivityDao;

    /**
     * user_daily_score / user_daily_activity를 diet 기반으로 채운다.
     * - daysRange: 최근 N일을 훑으면서 diet가 있는 날짜만 생성
     */
    @Transactional
    public void seedDailyScoreAndActivity(List<Long> userIds, int daysRange) {

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(daysRange);

        for (Long userId : userIds) {
            // dailyKcal 스냅샷: body_info 있으면 사용
            Integer dailyKcal = null;
            UserBodyInfo body = userBodyInfoDao.findByUserId(userId);
            if (body != null) {
                dailyKcal = body.getDailyKcal();
            }

            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
                UserDailyNutrition agg = userDailyNutritionDao.aggregateByUserAndDate(userId, d);

                // 그날 diet가 없다면 score/activity도 굳이 만들 필요 없음(원하면 0점으로 만들 수도 있음)
                if (agg == null || agg.getMealCount() == null || agg.getMealCount() == 0) {
                    continue;
                }

                // 1) user_daily_score upsert
                BigDecimal totalKcal = nz(agg.getTotalKcal()).setScale(3, RoundingMode.HALF_UP);

                int score = calcScoreHeuristic(dailyKcal, totalKcal);
                String grade = grade(score);
                String feedback = feedback(grade);

                UserDailyScore row = new UserDailyScore();
                row.setUserId(userId);
                row.setDate(d);
                row.setScore(score);
                row.setGrade(grade);
                row.setFeedback(feedback);
                row.setDailyKcal(dailyKcal);
                row.setTotalKcal(totalKcal.doubleValue());

                userDailyScoreDao.upsert(row);

                // 2) user_daily_activity insert/update (diet_logged = 1)
                UserDailyActivity act = userDailyActivityDao.findByUserAndDate(userId, d);
                if (act == null) {
                    act = new UserDailyActivity();
                    act.setUserId(userId);
                    act.setDate(d);
                    act.setDietLogged(true);
                    act.setWorkoutLogged(false);
                    act.setChallengeDone(false);
                    userDailyActivityDao.insert(act);
                } else {
                    act.setDietLogged(true);
                    userDailyActivityDao.update(act);
                }

            }
        }
    }

    // ------- 점수 휴리스틱 (seed용) -------
    private int calcScoreHeuristic(Integer dailyKcal, BigDecimal totalKcal) {
        // dailyKcal 없으면 랜덤이되 totalKcal 기반으로 대충 그럴싸하게
        if (dailyKcal == null || dailyKcal <= 0) {
            int base = faker.number().numberBetween(55, 96);
            // kcal 높을수록 약간 감점
            if (totalKcal.doubleValue() > 2200) base -= faker.number().numberBetween(5, 16);
            return clamp(base, 0, 100);
        }

        double diff = totalKcal.doubleValue() - dailyKcal;
        // dailyKcal 근처면 고득점, 많이 초과/미달이면 감점
        int score = 90;

        double abs = Math.abs(diff);
        if (abs <= 100) score = 95;
        else if (abs <= 300) score = 88;
        else if (abs <= 500) score = 78;
        else if (abs <= 800) score = 65;
        else score = 50;

        // 초과는 조금 더 엄격하게
        if (diff > 0) score -= 3;

        // 약간 랜덤 흔들기
        score += faker.number().numberBetween(-5, 6);

        return clamp(score, 0, 100);
    }

    private String grade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    private String feedback(String grade) {
        return switch (grade) {
            case "A" -> "아주 좋아요. 균형이 잘 맞아요.";
            case "B" -> "좋아요. 조금만 더 조절해봐요.";
            case "C" -> "무난해요. 단백질/채소 비중을 늘려보세요.";
            case "D" -> "개선 여지가 있어요. 당/나트륨을 줄여보세요.";
            default -> "식단 구성이 불균형해요. 작은 목표부터 시작해봐요.";
        };
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
