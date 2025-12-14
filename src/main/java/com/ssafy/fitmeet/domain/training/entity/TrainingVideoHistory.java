package com.ssafy.fitmeet.domain.training.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingVideoHistory {

    private Long id;
    private Long userId;
    private Long videoId;
    private LocalDate activityDate;
    private Integer completed; // 1: 완료, 0: 미완
    private Integer durationSec;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
