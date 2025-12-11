package com.ssafy.fitmeet.domain.diet.dao;

import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserDailyNutritionDao {

    UserDailyNutrition findByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    int insert(UserDailyNutrition daily);

    int update(UserDailyNutrition daily);

    int deleteByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    UserDailyNutrition aggregateByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<UserDailyNutrition> findRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
