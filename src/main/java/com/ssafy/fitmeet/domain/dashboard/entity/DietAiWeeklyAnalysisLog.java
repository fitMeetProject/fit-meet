package com.ssafy.fitmeet.domain.dashboard.entity;

import lombok.*;
import java.time.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietAiWeeklyAnalysisLog {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestSystemPrompt;
    private String requestUserPrompt;
    private String responseJson; // JSON 문자열 그대로 저장
    private Integer weeklyScore;
    private String status; // SUCCESS / FALLBACK / FAIL
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
