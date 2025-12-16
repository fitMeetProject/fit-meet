package com.ssafy.fitmeet.domain.training.dao;

import com.ssafy.fitmeet.domain.training.entity.TrainingVideoHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TrainingVideoHistoryDao {

    TrainingVideoHistory findByUserAndVideoAndDate(@Param("userId") Long userId, @Param("videoId") Long videoId, @Param("activityDate") LocalDate activityDate);

    int insertHistory(TrainingVideoHistory history);

    int updateHistory(TrainingVideoHistory history);

    List<TrainingVideoHistory> findByUserAndDate(@Param("userId") Long userId, @Param("activityDate") LocalDate activityDate);

    int countCompletedByUserAndDate(Long userId, LocalDate activityDate);

}
