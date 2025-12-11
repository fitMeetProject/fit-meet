package com.ssafy.fitmeet.domain.dashboard.web;

import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.*;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.diet.service.UserDailyNutritionService;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.service.UserService;
import com.ssafy.fitmeet.global.response.Response;
import com.ssafy.fitmeet.global.response.ResponseUtil;
import com.ssafy.fitmeet.global.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "DashBoard", description = "대시보드 관련 API")
public class DashboardRestController {

    private final UserService userService;
    private final UserDailyNutritionService userDailyNutritionService;

    /**
     * 오늘 요약 (대시보드 상단 카드용)
     */
    @GetMapping("/today-nutrition")
    @Operation(summary = "오늘 식단 요약 ( 영양소 )", description = "대시보드 상단 카드용")
    public ResponseEntity<Response<TodayNutritionResponse>> getTodayNutrition(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        LocalDate today = LocalDate.now();

        // 신체 정보 (BMR, dailyKcal 계산 값이 이미 저장되어 있다고 가정)
        UserBodyInfo bodyInfo = userService.findByUserId(userId);

        Integer bmr = (bodyInfo != null) ? bodyInfo.getBmr() : null;
        Integer dailyKcal = (bodyInfo != null) ? bodyInfo.getDailyKcal() : null;

        // 오늘 하루 섭취 요약
        UserDailyNutrition nut = userDailyNutritionService.getDaily(userId, today);

        double todayKcal = (nut != null && nut.getTotalKcal() != null) ? nut.getTotalKcal().doubleValue() : 0.0;
        double todayCarbo = (nut != null && nut.getTotalCarbohydrate() != null) ? nut.getTotalCarbohydrate().doubleValue() : 0.0;
        double todayProtein = (nut != null && nut.getTotalProtein() != null) ? nut.getTotalProtein().doubleValue() : 0.0;
        double todayFat = (nut != null && nut.getTotalFat() != null) ? nut.getTotalFat().doubleValue() : 0.0;
        double todaySugar = (nut != null && nut.getTotalSugar() != null) ? nut.getTotalSugar().doubleValue() : 0.0;
        double todaySodium = (nut != null && nut.getTotalSodium() != null) ? nut.getTotalSodium().doubleValue() : 0.0;
        double kcalRatio = 0.0;

        if (dailyKcal != null && dailyKcal > 0) {
            kcalRatio = todayKcal / dailyKcal;
        }

        TodayNutritionResponse dto = new TodayNutritionResponse(today, bmr, dailyKcal, todayKcal, todayCarbo, todayProtein, todayFat, todaySugar, todaySodium, kcalRatio);

        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    /**
     * 기간별 일자별 섭취 요약 (그래프용)
     * 최근 7일/30일 등
     */
    @GetMapping("/nutrition-range")
    @Operation(summary = "기간별 일자별 섭취 요약", description = "최근 7일/30일 등")
    public ResponseEntity<Response<NutritionRangeResponse>> getNutritionRange(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = userDetails.getId();

        List<UserDailyNutrition> list = userDailyNutritionService.getRange(userId, startDate, endDate);

        List<DailyNutritionPoint> items = list.stream()
                .map(n -> new DailyNutritionPoint(
                        n.getDate(),
                        n.getTotalKcal() != null ? n.getTotalKcal().doubleValue() : 0.0,
                        n.getTotalCarbohydrate() != null ? n.getTotalCarbohydrate().doubleValue() : 0.0,
                        n.getTotalProtein() != null ? n.getTotalProtein().doubleValue() : 0.0,
                        n.getTotalFat() != null ? n.getTotalFat().doubleValue() : 0.0,
                        n.getTotalSugar() != null ? n.getTotalSugar().doubleValue() : 0.0,
                        n.getTotalSodium() != null ? n.getTotalSodium().doubleValue() : 0.0
                ))
                .toList();

        NutritionRangeResponse dto = new NutritionRangeResponse(startDate, endDate, items);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }
}
