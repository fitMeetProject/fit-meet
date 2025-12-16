package com.ssafy.fitmeet.domain.dashboard.dao;

import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserDailyScoreDao {
    UserDailyScore findByUserAndDate(Long userId, LocalDate date);

    void upsert(UserDailyScore score);

    List<UserDailyScore> findByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
