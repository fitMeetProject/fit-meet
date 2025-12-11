package com.ssafy.fitmeet.domain.user.service;

import com.ssafy.fitmeet.domain.user.dto.ActivityDto.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserDailyActivityService {
    void markDietLogged(Long userId, LocalDate date);

    void unmarkDietLogged(Long userId, LocalDate date);

    List<DietActivityDay> getDietActivityRange(Long userId, LocalDate fromDate, LocalDate toDate);

    DietStreakInfo getCurrentDietStreak(Long userId);
}
