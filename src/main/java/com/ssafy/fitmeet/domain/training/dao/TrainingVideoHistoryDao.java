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

    int countCompletedByUserAndDate(@Param("userId") Long userId, @Param("activityDate") LocalDate activityDate);

    /** 오늘 완료(completed=1)한 video_id 목록 */
    List<Long> findCompletedVideoIdsByUserAndDate(@Param("userId") Long userId, @Param("activityDate") LocalDate activityDate);

    /** 과거 포함(누적) completed=1 이력이 있는 video_id 목록 */
    List<Long> findEverCompletedVideoIdsByUser(@Param("userId") Long userId);
}
