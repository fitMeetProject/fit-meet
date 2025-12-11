package com.ssafy.fitmeet.domain.user.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserDailyActivity {

    private Long userId;
    private LocalDate date;

    private Boolean dietLogged; // 식단 기록 여부
    private Boolean workoutLogged; // 운동 기록 여부
    private Boolean challengeDone; // 챌린지 완료 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
