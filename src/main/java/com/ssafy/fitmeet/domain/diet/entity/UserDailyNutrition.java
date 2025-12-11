package com.ssafy.fitmeet.domain.diet.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserDailyNutrition {

    private Long userId;
    private LocalDate date;

    private BigDecimal totalKcal;
    private BigDecimal totalCarbohydrate;
    private BigDecimal totalProtein;
    private BigDecimal totalFat;
    private BigDecimal totalSugar;
    private BigDecimal totalSodium;

    private Integer dietScoreAvg;
    private Integer dietScoreSum;
    private Integer mealCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
