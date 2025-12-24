package com.ssafy.fitmeet.domain.user.web;

import com.ssafy.fitmeet.domain.user.dto.ActivityDto.*;
import com.ssafy.fitmeet.domain.user.service.UserDailyActivityService;
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
@RequestMapping("/api/user-activity")
@RequiredArgsConstructor
@Tag(name = "Streak", description = "사용자 기준 스트릭")
public class UserActivityRestController {

    private final UserDailyActivityService userDailyActivityService;

    /**
     * 현재 식단 스트릭 정보
     */
    @GetMapping("/diet/streak")
    @Operation(summary = "스트릭 정보", description = "오늘부터 뒤로 연속해서 dietLogged = true 인 날의 수 ( 100일 기준 )")
    public ResponseEntity<Response<DietStreakInfo>> getDietStreak(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();

        var streakInfo = userDailyActivityService.getCurrentDietStreak(userId);
        DietStreakInfo dto = new DietStreakInfo(streakInfo.streakCount(), streakInfo.startDate(), streakInfo.endDate());

        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    /**
     * 기간별 식단 활동 여부 (캘린더/스트릭 뷰용)
     * - Vue에서 색칠 여부를 boolean으로 판단해서 사용
     */
    @GetMapping("/diet/activity-range")
    @Operation(summary = "기간별 식단 활동 여부", description = "Vue에서 색칠 여부를 boolean으로 판단해서 사용")
    public ResponseEntity<Response<DietActivityRangeResponse>> getDietActivityRange(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = userDetails.getId();

        List<DietActivityDay> days = userDailyActivityService.getDietActivityRange(userId, startDate, endDate);
        DietActivityRangeResponse dto = new DietActivityRangeResponse(startDate, endDate, days);

        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }
}
