package com.ssafy.fitmeet.domain.diet.service.score;

import com.ssafy.fitmeet.domain.diet.entity.Diet;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;

import java.math.BigDecimal;

/**
 * 점수 계산 알고리즘
 *
 * 한 끼 식단 점수 계산 전담 클래스
 * 식사의 질(공통) + 사용자 신체정보 기반 적합도(개인화) 구조
 *
 * 점수 구성 (총 100점)
 *  - 공통 품질 점수 : 70점
 *      · 탄/단/지 비율 균형
 *      · 당/나트륨 과다 섭취 페널티
 *  - 개인화 적합도 : 30점
 *      · 사용자 dailyKcal 기준, 한 끼 칼로리 적합도
 *      · 사용자 체중 기준, 단백질 섭취 적합도
 */
public class DietScoreCalculator {

    /**
     * 계산 결과 DTO (Service → DietScore 생성용)
     */
    public record ScoreResult(
            int score, // 0~100
            String grade, // A~E
            String feedback // 사용자 피드백 문구
    ) {}

    /**
     * 개인화 한 끼 점수 계산 메인 진입점
     */
    public static ScoreResult calculate(Diet diet, UserBodyInfo bodyInfo) {

        if (diet == null) {
            return new ScoreResult(0, "E", "식단 정보가 없어 점수를 계산할 수 없어요.");
        }

         // 식단 총량 값 추출
        double kcal = toDouble(diet.getTotalKcal());
        double carbG = toDouble(diet.getTotalCarbohydrate());
        double proteinG = toDouble(diet.getTotalProtein());
        double fatG = toDouble(diet.getTotalFat());
        double sugarG = toDouble(diet.getTotalSugar());
        double sodiumMg = toDouble(diet.getTotalSodium());

        // 공통 품질 점수 (0 ~ 70)
        int macroScore = calcMacroBalanceScore(carbG, proteinG, fatG); // 0~70
        int penaltyScore = calcPenaltyScore(sugarG, sodiumMg); // 0~20 (감점)

        int qualityScore = clamp(macroScore - penaltyScore, 0, 70);

        // 개인화 적합도 점수 (0 ~ 30)
        int personalScore = calcPersonalScore(kcal, proteinG, diet.getMealType(), bodyInfo);

        // 최종 점수
        int totalScore = clamp(qualityScore + personalScore, 0, 100);
        String grade = gradeOf(totalScore);
        String feedback = feedbackOf(totalScore, qualityScore, personalScore, sugarG, sodiumMg, bodyInfo);

        return new ScoreResult(totalScore, grade, feedback);
    }

    /**
     * 탄/단/지 비율 균형 점수 (0 ~ 70)
     *
     * 기준 (일반적인 건강 권장 범위)
     *  - 탄수화물 : 45 ~ 65%
     *  - 단백질   : 10 ~ 35%
     *  - 지방     : 20 ~ 35%
     *
     * 범위 안이면 만점, 벗어나면 거리 비례 감점
     */
    private static int calcMacroBalanceScore(double carbG, double proteinG, double fatG) {

        double carbKcal = carbG * 4.0;
        double proteinKcal = proteinG * 4.0;
        double fatKcal = fatG * 9.0;

        double totalKcal = carbKcal + proteinKcal + fatKcal;
        if (totalKcal <= 0) return 0;

        double carbRatio = carbKcal / totalKcal;
        double proteinRatio = proteinKcal / totalKcal;
        double fatRatio = fatKcal / totalKcal;

        double carbFit = rangeFit(carbRatio, 0.45, 0.65, 0.20);
        double proteinFit = rangeFit(proteinRatio, 0.10, 0.35, 0.20);
        double fatFit = rangeFit(fatRatio, 0.20, 0.35, 0.20);
        double avgFit = (carbFit + proteinFit + fatFit) / 3.0;
        return (int) Math.round(avgFit * 70.0);
    }

    /**
     * 당 / 나트륨 과다 섭취 페널티 (0 ~ 20)
     */
    private static int calcPenaltyScore(double sugarG, double sodiumMg) {
        double sugarPenalty = penaltyLinear(sugarG, 15.0, 50.0, 10.0); // 최대 10점
        double sodiumPenalty = penaltyLinear(sodiumMg, 700.0, 2000.0, 10.0); // 최대 10점

        return (int) Math.round(clampDouble(sugarPenalty + sodiumPenalty, 0, 20));
    }

    /**
     * 개인화 점수 (0 ~ 30)
     *
     * dailyKcal 기준 한 끼 칼로리 적합도 (0~20)
     * 체중 기반 단백질 섭취 적합도 (0~10)
     */
    private static int calcPersonalScore(double mealKcal, double proteinG, String mealType, UserBodyInfo bodyInfo) {
        if (bodyInfo == null) return 0;

        int score = 0;
        // 한 끼 칼로리 적합도
        if (bodyInfo.getDailyKcal() != null && bodyInfo.getDailyKcal() > 0 && mealKcal > 0) {
            double mealRatio = mealRatio(mealType);
            double targetMealKcal = bodyInfo.getDailyKcal() * mealRatio;
            // 목표 대비 ±35%까지는 관대하게
            double fit = closenessFit(mealKcal, targetMealKcal, 0.35);
            score += (int) Math.round(fit * 20.0);
        }

        // 체중 기반 단백질 적합도
        if (bodyInfo.getWeightKg() != null && bodyInfo.getWeightKg() > 0 && proteinG > 0) {
            // 일일 단백질 목표 (MVP 기준)
            double dailyProteinTarget = bodyInfo.getWeightKg() * 1.2;
            // 한 끼 목표 = 일일 / 3
            double mealProteinTarget = dailyProteinTarget / 3.0;
            double fit = saturatingFit(proteinG, mealProteinTarget * 0.6, mealProteinTarget);

            score += (int) Math.round(fit * 10.0);
        }

        return clamp(score, 0, 30);
    }

    /**
     * 식사 타입별 한 끼 칼로리 비중
     */
    private static double mealRatio(String mealType) {
        if (mealType == null) return 0.30;
        return switch (mealType.toUpperCase()) {
            case "A" -> 0.25; // 아침
            case "B" -> 0.35; // 점심
            case "C" -> 0.30; // 저녁
            case "D" -> 0.10; // 간식
            case "E" -> 0.10; // 야식
            default  -> 0.30;
        };
    }

    private static String gradeOf(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    private static String feedbackOf(int total, int quality, int personal, double sugarG, double sodiumMg, UserBodyInfo bodyInfo) {
        if (sugarG >= 30) return "당류가 다소 높아요. 음료나 디저트를 줄여보세요.";
        if (sodiumMg >= 1200) return "나트륨이 높아요. 국물이나 가공식품을 주의해보세요.";

        if (bodyInfo != null && personal <= 10) {
            return "개인 목표 대비 아쉬운 식사예요. 다음 끼니에서 조절해보세요.";
        }

        if (total >= 85) return "균형 잡힌 아주 좋은 식사예요!";
        if (quality < 40) return "탄·단·지 균형을 조금만 더 맞춰보세요.";

        return "전반적으로 무난한 식사예요.";
    }

    private static double rangeFit(double v, double min, double max, double tol) {
        if (v >= min && v <= max) return 1.0;
        double dist = (v < min) ? (min - v) : (v - max);
        return clampDouble(1.0 - (dist / tol), 0.0, 1.0);
    }

    private static double closenessFit(double value, double target, double tolRatio) {
        if (target <= 0) return 0.0;
        double diff = Math.abs(value - target) / target;
        if (diff <= tolRatio) return 1.0;
        return clampDouble(1.0 - ((diff - tolRatio) / (1.0 - tolRatio)), 0.0, 1.0);
    }

    private static double saturatingFit(double value, double start, double end) {
        if (value <= start) return 0.0;
        if (value >= end) return 1.0;
        return (value - start) / (end - start);
    }

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
