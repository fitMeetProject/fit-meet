package com.ssafy.fitmeet.domain.dashboard.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyScore {

    private Long id;
    private Long userId;
    private LocalDate date;
    private Integer score;
    private String grade;
    private String feedback;
    private Integer dailyKcal; // 계산 당시 기준
    private Double totalKcal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
