package com.ssafy.fitmeet.domain.dashboard.service.score;

import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;

import java.math.BigDecimal;

/**
 * DashboardDailyScoreCalculator
 *
 * "하루 점수" 산정 전용 계산기.
 *
 * 점수(0~100) 구성:
 *  - 목표 달성도(70)
 *    1) kcal 적합도: 0~30  (dailyKcal 대비)
 *    2) 단백질 적합도: 0~20 (체중 기반)
 *    3) 당/나트륨 관리: 0~20 (과다 섭취 페널티)
 *
 *  - 기록 성실도(30)
 *    4) 식단 기록: 0 or 15
 *    5) 운동 기록: 0 or 15
 *
 * 왜 이렇게?
 *  - 대시보드/차트는 "하루 목표 달성"이 핵심이므로 개인화 비중을 높게 둠
 *  - 그래도 UX상 기록 유도를 위해 성실도 점수를 별도로 부여
 */
public class DashboardDailyScoreCalculator {

    public record Result(int score, String grade, String feedback) {}

    public static Result calculate(UserDailyNutrition dailyNutrition, UserBodyInfo bodyInfo, boolean dietLogged, boolean workoutLogged) {
        double totalKcal = toDouble(dailyNutrition != null ? dailyNutrition.getTotalKcal() : null);
        double totalProteinG = toDouble(dailyNutrition != null ? dailyNutrition.getTotalProtein() : null);
        double totalSugarG = toDouble(dailyNutrition != null ? dailyNutrition.getTotalSugar() : null);
        double totalSodiumMg = toDouble(dailyNutrition != null ? dailyNutrition.getTotalSodium() : null);

        Integer dailyKcalTarget = (bodyInfo != null) ? bodyInfo.getDailyKcal() : null;
        Double weightKg = (bodyInfo != null) ? bodyInfo.getWeightKg() : null;

        // 1. 목표 달성도(70)
        int kcalScore = calcKcalFitScore(totalKcal, dailyKcalTarget); // 0~30
        int proteinScore = calcProteinFitScore(totalProteinG, weightKg); // 0~20
        int sugarSodiumScore = calcSugarSodiumScore(totalSugarG, totalSodiumMg); // 0~20

        int goalScore = clamp(kcalScore + proteinScore + sugarSodiumScore, 0, 70);

        // 2. 기록 성실도(30)
        int logScore = 0;
        logScore += dietLogged ? 15 : 0;
        logScore += workoutLogged ? 15 : 0;

        // 3. 최종 점수
        int total = clamp(goalScore + logScore, 0, 100);
        String grade = gradeOf(total);
        String feedback = feedbackOf(
                total,
                goalScore,
                logScore,
                totalKcal,
                dailyKcalTarget,
                totalProteinG,
                weightKg,
                totalSugarG,
                totalSodiumMg,
                dietLogged,
                workoutLogged
        );

        return new Result(total, grade, feedback);
    }

    /**
     * kcal 적합도 (0~30)
     * - 목표(dailyKcal)에 가까울수록 점수 높음
     * - ±20% 이내는 거의 만점, 그 밖은 선형 감점
     * !!!! 신체정보가 없거나 목표가 없으면 0점 처리
     */
    private static int calcKcalFitScore(double totalKcal, Integer dailyKcalTarget) {
        if (dailyKcalTarget == null || dailyKcalTarget <= 0) return 0;
        if (totalKcal <= 0) return 0;

        // 목표 대비 오차 허용(20%)
        double fit = closenessFit(totalKcal, dailyKcalTarget.doubleValue(), 0.20);
        return (int) Math.round(fit * 30.0);
    }

    /**
     * 단백질 적합도 (0~20)
     * - 일일 목표 단백질: weightKg * 1.2g (MVP)
     * - 달성률이 60% 이하이면 0점에 가깝고, 100% 이상이면 만점에 가깝게
     *
     * 신체정보 없으면 0점 처리
     */
    private static int calcProteinFitScore(double totalProteinG, Double weightKg) {
        if (weightKg == null || weightKg <= 0) return 0;
        if (totalProteinG <= 0) return 0;

        double dailyProteinTarget = weightKg * 1.2; // MVP 기준(운동 강도/목표 반영은 다음 단계)
        // 60%~100% 구간에서 점수 증가, 100% 이상은 만점
        double fit = saturatingFit(totalProteinG, dailyProteinTarget * 0.6, dailyProteinTarget);
        return (int) Math.round(fit * 20.0);
    }

    /**
     * 당/나트륨 관리 점수 (0~20)
     * - 초과 시 감점하는 방식이 아니라, 관리 점수를 직접 산정(20점 만점에서 페널티 차감)
     *
     * 기준(MVP, 서비스 성격 맞춰 조정 가능):
     *  - 당: 50g 초과부터 감점, 100g에서 최대 감점
     *  - 나트륨: 2000mg 초과부터 감점, 4000mg에서 최대 감점
     */
    private static int calcSugarSodiumScore(double sugarG, double sodiumMg) {
        // 최대 감점 각각 10점씩 → 총 20점
        double sugarPenalty = penaltyLinear(sugarG, 50.0, 100.0, 10.0);
        double sodiumPenalty = penaltyLinear(sodiumMg, 2000.0, 4000.0, 10.0);

        double totalPenalty = clampDouble(sugarPenalty + sodiumPenalty, 0, 20);
        double score = 20.0 - totalPenalty;
        return (int) Math.round(clampDouble(score, 0, 20));
    }

    // Feedback / Grade
    private static String gradeOf(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    /**
     * 피드백 생성 규칙
     * - 경고(과다) → 기록 유도 → 목표 조절 → 칭찬 순으로 우선순위를 둠
     */
    private static String feedbackOf(int total, int goalScore, int logScore, double totalKcal, Integer dailyKcalTarget, double totalProteinG, Double weightKg, double sugarG, double sodiumMg, boolean dietLogged, boolean workoutLogged) {
        // 1. 과다 섭취 경고 우선
        if (sugarG >= 100) return "당류 섭취가 매우 높아요. 음료/디저트 섭취를 강하게 줄여보세요.";
        if (sodiumMg >= 4000) return "나트륨 섭취가 매우 높아요. 국물/가공식품을 강하게 줄여보세요.";
        if (sugarG >= 70) return "당류가 높은 편이에요. 간식/음료를 조절해보세요.";
        if (sodiumMg >= 2500) return "나트륨이 높은 편이에요. 국물/가공식품을 조절해보세요.";

        // 2. 기록 유도
        if (!dietLogged && !workoutLogged) return "아직 기록이 부족해요. 식단/운동을 기록하면 점수가 올라가요.";
        if (!dietLogged) return "식단 기록이 없어요. 오늘 섭취한 식단을 기록해보세요.";
        if (!workoutLogged) return "운동 기록이 없어요. 가벼운 활동이라도 기록해보세요.";

        // 3. 목표 대비 kcal/단백질 코멘트(개인화 가능할 때만)
        if (dailyKcalTarget != null && dailyKcalTarget > 0 && totalKcal > 0) {
            double ratio = totalKcal / dailyKcalTarget;
            if (ratio >= 1.30) return "오늘은 목표 칼로리보다 많이 섭취했어요. 내일은 조금 조절해보세요.";
            if (ratio <= 0.70) return "오늘은 목표 칼로리보다 적게 섭취했어요. 내일은 영양을 보완해보세요.";
        }
        if (weightKg != null && weightKg > 0) {
            double targetProtein = weightKg * 1.2;
            if (totalProteinG < targetProtein * 0.6) return "단백질이 부족해요. 다음 식사에 단백질을 추가해보세요.";
        }

        // 4. 결과 기반
        if (total >= 85) return "목표에 잘 맞는 하루예요. 좋은 흐름을 유지해보세요.";
        if (goalScore < 40) return "목표 대비 아쉬운 부분이 있어요. 다음 끼니에서 균형을 맞춰보세요.";
        return "전반적으로 무난한 하루예요. 내일은 한 가지 지표만 개선해보세요.";
    }

    /**
     * target 대비 오차 허용률(tolRatio) 내면 1, 이후 선형 감소
     * tolRatio=0.2면 ±20%까지는 거의 만점
     */
    private static double closenessFit(double value, double target, double tolRatio) {
        if (target <= 0) return 0.0;
        double diff = Math.abs(value - target) / target;
        if (diff <= tolRatio) return 1.0;
        // tol 이후는 (diff=1.0)까지 선형 감소
        double fit = 1.0 - ((diff - tolRatio) / (1.0 - tolRatio));
        return clampDouble(fit, 0.0, 1.0);
    }

    /**
     * value가 start 이하이면 0, end 이상이면 1, 중간 선형
     */
    private static double saturatingFit(double value, double start, double end) {
        if (end <= start) return (value >= end) ? 1.0 : 0.0;
        if (value <= start) return 0.0;
        if (value >= end) return 1.0;
        return (value - start) / (end - start);
    }

    /**
     * value가 start 이하이면 0, end 이상이면 maxPenalty, 중간 선형
     */
    private static double penaltyLinear(double value, double start, double end, double maxPenalty) {
        if (value <= start) return 0.0;
        if (value >= end) return maxPenalty;
        return (value - start) / (end - start) * maxPenalty;
    }

    private static double toDouble(BigDecimal v) {
        return v == null ? 0.0 : v.doubleValue();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
