package com.ssafy.fitmeet.domain.diet.service;

import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;

import java.time.LocalDate;
import java.util.List;

public interface UserDailyNutritionService {

    /**
     * 특정 유저 + 날짜에 대해 diet/diet_score 기준으로 다시 집계해서
     * user_daily_nutrition 테이블에 반영.
     */
    void recalcDailyNutrition(Long userId, LocalDate date);

    /**
     * 특정 유저 + 날짜 요약 조회 (없으면 null)
     */
    UserDailyNutrition getDaily(Long userId, LocalDate date);

    /**
     * 특정 기간 [startDate ~ endDate] 의 요약 리스트 조회
     * (date 오름차순)
     */
    List<UserDailyNutrition> getRange(Long userId, LocalDate startDate, LocalDate endDate);
}
