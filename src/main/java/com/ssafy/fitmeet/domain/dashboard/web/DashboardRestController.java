package com.ssafy.fitmeet.domain.dashboard.web;

import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.*;
import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import com.ssafy.fitmeet.domain.dashboard.service.UserDailyScoreService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "DashBoard", description = "대시보드 관련 API")
public class DashboardRestController {

    private final UserService userService;
    private final UserDailyNutritionService userDailyNutritionService;
    private final UserDailyScoreService userDailyScoreService;

    /**
     * 오늘 요약 (대시보드 상단 카드용)
     */
    @GetMapping("/today-nutrition")
    @Operation(summary = "오늘 식단 요약 ( 영양소 )", description = "대시보드 상단 카드용")
    public ResponseEntity<Response<TodayNutritionResponse>> getTodayNutrition(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        LocalDate today = LocalDate.now();

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
     * 오늘 점수 (호출 시 리프레시)
     */
    @GetMapping("/today-score")
    @Operation(summary = "오늘 하루 점수(리프레시)", description = "오늘 UserDailyNutrition/BodyInfo/Activity 기반으로 점수 계산 후 upsert")
    public ResponseEntity<Response<TodayScoreResponse>> getTodayScore(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        LocalDate today = LocalDate.now();

        UserDailyScore s = userDailyScoreService.refreshDailyScore(userId, today);

        TodayScoreResponse dto = TodayScoreResponse.from(s);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    /**
     * 주간 점수 차트 (일별 + 평균)
     * - endDate 기준 최근 7일
     */
    @GetMapping("/score-week")
    @Operation(summary = "주간 점수(일별 + 평균)", description = "endDate 기준 7일치 점수 리프레시 후 일별 점수 + 평균 반환")
    public ResponseEntity<Response<WeekScoreResponse>> getWeekScore(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = userDetails.getId();
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        LocalDate start = end.minusDays(6);

        List<UserDailyScore> list = userDailyScoreService.refreshWeek(userId, end);

        List<ScorePoint> items = list.stream()
                .map(s -> new ScorePoint(s.getDate(), s.getScore()))
                .toList();

        int avg = averageScore(items);

        WeekScoreResponse dto = new WeekScoreResponse(start, end, avg, items);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    /**
     * 월간 점수 차트 (주별 평균 + 월 평균)
     * - year/month 없으면 현재 월
     * - ISO week 기준으로 주차 묶되, 주 시작일(월)을 key로 평균 계산
     */
    @GetMapping("/score-month")
    @Operation(summary = "월간 점수(주별 평균 + 월 평균)", description = "해당 월 일별 점수 리프레시 후 ISO 주차별 평균 반환")
    public ResponseEntity<Response<MonthScoreResponse>> getMonthScore(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam(value = "year", required = false) Integer year, @RequestParam(value = "month", required = false) Integer month) {
        Long userId = userDetails.getId();
        LocalDate now = LocalDate.now();
        int y = (year != null) ? year : now.getYear();
        int m = (month != null) ? month : now.getMonthValue();

        YearMonth ym = YearMonth.of(y, m);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<UserDailyScore> daily = userDailyScoreService.refreshMonth(userId, y, m);

        Map<LocalDate, List<UserDailyScore>> byWeekStart = new HashMap<>();
        for (UserDailyScore s : daily) {
            LocalDate weekStart = s.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            byWeekStart.computeIfAbsent(weekStart, k -> new ArrayList<>()).add(s);
        }

        List<LocalDate> weekStarts = byWeekStart.keySet().stream().sorted().toList();

        List<WeekAveragePoint> weeks = new ArrayList<>();
        for (LocalDate ws : weekStarts) {
            List<UserDailyScore> bucket = byWeekStart.get(ws);

            int avg = (int) Math.round(bucket.stream().mapToInt(UserDailyScore::getScore).average().orElse(0));
            LocalDate we = ws.plusDays(6);

            LocalDate displayStart = (ws.isBefore(start)) ? start : ws;
            LocalDate displayEnd = (we.isAfter(end)) ? end : we;

            weeks.add(new WeekAveragePoint(displayStart, displayEnd, avg));
        }

        int monthAvg = (int) Math.round(daily.stream().mapToInt(UserDailyScore::getScore).average().orElse(0));

        MonthScoreResponse dto = new MonthScoreResponse(y, m, monthAvg, weeks);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    private int averageScore(List<ScorePoint> items) {
        if (items == null || items.isEmpty()) return 0;
        return (int) Math.round(items.stream().mapToInt(ScorePoint::score).average().orElse(0));
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
