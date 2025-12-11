package com.ssafy.fitmeet.domain.user.dao;

import com.ssafy.fitmeet.domain.user.entity.UserDailyActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserDailyActivityDao {

    UserDailyActivity findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    int insert(UserDailyActivity activity);

    int update(UserDailyActivity activity);

    // 특정 유저의 최근 n일 활동 기록 (스트릭 계산용)
    List<UserDailyActivity> findBetweenDates(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
