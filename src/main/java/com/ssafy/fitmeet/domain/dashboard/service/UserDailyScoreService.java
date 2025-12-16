package com.ssafy.fitmeet.domain.dashboard.service;

import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;

import java.time.LocalDate;
import java.util.List;

public interface UserDailyScoreService {

    // 호출 시점에 점수 리프레시
    UserDailyScore refreshDailyScore(Long userId, LocalDate date);

    // 주간(일별) 차트용: endDate 기준 7일 리프레시 + 반환
    List<UserDailyScore> refreshWeek(Long userId, LocalDate endDate);

    //  월간(주별) 차트용: 해당 월 전체 일자 리프레시 + 반환
    List<UserDailyScore> refreshMonth(Long userId, int year, int month);
}
