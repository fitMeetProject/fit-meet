package com.ssafy.fitmeet.domain.analysis.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisLog {
    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestSystemPrompt;
    private String requestUserPrompt;
    private String responseJson;
    private Integer weeklyScore;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

