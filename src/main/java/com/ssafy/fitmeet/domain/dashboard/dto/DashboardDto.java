package com.ssafy.fitmeet.domain.dashboard.dto;

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
}
