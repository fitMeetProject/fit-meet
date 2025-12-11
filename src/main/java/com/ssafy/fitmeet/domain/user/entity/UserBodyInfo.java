package com.ssafy.fitmeet.domain.user.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class UserBodyInfo {

    private Long id;
    private Long userId;
    private Double heightCm;
    private Double weightKg;
    private Double targetWeightKg;
    private String gender; // MALE / FEMALE / OTHER
    private LocalDate birthDate;
    private String activityLevel; // LOW / MID / HIGH
    private Integer bmr; // 기초대사량
    private Integer dailyKcal; // 하루 권장 섭취 칼로리
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
