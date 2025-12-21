package com.ssafy.fitmeet.domain.analysis.web;

import com.ssafy.fitmeet.domain.analysis.dto.AnalysisLogDto.WeeklyAiAnalysisLogResponse;
import com.ssafy.fitmeet.domain.analysis.service.AnalysisLogService;
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
@RequestMapping("/api/weekly-analysis/logs")
@RequiredArgsConstructor
@Tag(name = "Diet AI 주간 분석 로그")
public class AnalysisLogRestController {

    private final AnalysisLogService service;

    @GetMapping
    @Operation(summary = "주간 AI 분석 로그 목록 조회 (최신순)")
    public ResponseEntity<Response<List<WeeklyAiAnalysisLogResponse>>> getLogs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        List<WeeklyAiAnalysisLogResponse> dto = service.getLogs(userId);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    @GetMapping("/range")
    @Operation(summary = "주간 AI 분석 로그 기간 조회 (endDate 기준)")
    public ResponseEntity<?> getLogsByDate(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long userId = userDetails.getId();
        List<WeeklyAiAnalysisLogResponse> dto = service.getLogsByDate(userId, from, to);
        return ResponseEntity.ok(ResponseUtil.ok(dto));
    }

    @DeleteMapping("/{logId}")
    @Operation(summary = "주간 AI 분석 로그 삭제")
    public ResponseEntity<Response<Void>> deleteLog(@AuthenticationPrincipal CustomUserDetails user, @PathVariable Long logId) {
        service.deleteLog(user.getId(), logId);
        return ResponseEntity.ok(ResponseUtil.ok("로그 삭제 완료", null));
    }
}
