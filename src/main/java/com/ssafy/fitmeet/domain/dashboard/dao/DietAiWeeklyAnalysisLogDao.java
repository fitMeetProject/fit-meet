package com.ssafy.fitmeet.domain.dashboard.dao;

import com.ssafy.fitmeet.domain.dashboard.entity.DietAiWeeklyAnalysisLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface DietAiWeeklyAnalysisLogDao {

    void insert(DietAiWeeklyAnalysisLog log);

    DietAiWeeklyAnalysisLog findLatestSuccess(@Param("userId") Long userId, @Param("endDate") LocalDate endDate);
}
