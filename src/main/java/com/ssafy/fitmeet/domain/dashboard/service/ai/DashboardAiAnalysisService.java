package com.ssafy.fitmeet.domain.dashboard.service.ai;

import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.WeeklyAiAnalysisResponse;

import java.time.LocalDate;

public interface DashboardAiAnalysisService {

    WeeklyAiAnalysisResponse analyzeWeekly(Long userId, LocalDate endDate);
}
