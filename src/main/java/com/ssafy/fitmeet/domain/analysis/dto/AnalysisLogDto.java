package com.ssafy.fitmeet.domain.analysis.dto;

import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AnalysisLogDto {

    public record WeeklyAiAnalysisLogResponse(
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            Integer weeklyScore,
            WeeklyAiResult analysis,
            String status,
            String errorMessage,
            LocalDateTime createdAt
    ) {}

}
