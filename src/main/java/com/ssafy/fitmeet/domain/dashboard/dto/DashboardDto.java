package com.ssafy.fitmeet.domain.dashboard.dto;

import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;

import java.time.LocalDate;
import java.util.List;

public class DashboardDto {

    /**
     * 오늘 요약
     */
    public record TodayNutritionResponse(
            LocalDate date,
            Integer bmr, // 기초대사량 (없으면 null)
            Integer dailyKcal, // 권장 섭취 칼로리 (없으면 null)
            Double todayKcal, // 오늘 섭취 칼로리 (없으면 0.0)
            Double todayCarbohydrate,
            Double todayProtein,
            Double todayFat,
            Double todaySugar,
            Double todaySodium,
            Double kcalRatio // todayKcal / dailyKcal (dailyKcal 없으면 0.0)
    ) {}

    /**
     * 일자별 섭취 요약 (그래프용)
     */
    public record DailyNutritionPoint(
            LocalDate date,
            Double totalKcal,
            Double totalCarbohydrate,
            Double totalProtein,
            Double totalFat,
            Double totalSugar,
            Double totalSodium
    ) {}

    /**
     * 기간별 섭취 요약 응답
     */
    public record NutritionRangeResponse(
            LocalDate fromDate,
            LocalDate toDate,
            List<DailyNutritionPoint> items
    ) {}

    /**
     * 오늘 점수 응답 (대시보드 상단 카드)
     */
    public record TodayScoreResponse(
            LocalDate date,
            Integer score,
            String grade,
            String feedback
    ) {
        public static TodayScoreResponse from(UserDailyScore s) {
            if (s == null) {
                return new TodayScoreResponse(LocalDate.now(), 0, "E", "점수 데이터가 없어요.");
            }
            return new TodayScoreResponse(s.getDate(), s.getScore(), s.getGrade(), s.getFeedback());
        }
    }

    /**
     * 주간 차트: 일별 점수 포인트
     * 프론트 라인차트에 그대로 사용 가능
     */
    public record ScorePoint(
            LocalDate date,
            Integer score
    ) {}

    /**
     * 주간 차트 응답
     * - startDate/endDate: 축 범위 표시용
     * - average: 주 평균
     * - items: 일별 점수
     */
    public record WeekScoreResponse(
            LocalDate startDate,
            LocalDate endDate,
            Integer average,
            List<ScorePoint> items
    ) {}

    /**
     * 월간 차트: 주별 평균 포인트
     * - weekStart/weekEnd: 주 라벨링(툴팁/범위 표시)
     */
    public record WeekAveragePoint(
            LocalDate weekStart,
            LocalDate weekEnd,
            Integer averageScore
    ) {}

    /**
     * 월간 차트 응답
     * - monthAverage: 월 평균
     * - weeks: 주별 평균 리스트
     */
    public record MonthScoreResponse(
            Integer year,
            Integer month,
            Integer monthAverage,
            List<WeekAveragePoint> weeks
    ) {}
}
