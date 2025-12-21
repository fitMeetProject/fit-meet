package com.ssafy.fitmeet.domain.analysis.dao;

import com.ssafy.fitmeet.domain.analysis.entity.AnalysisLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AnalysisLogDao {

    List<AnalysisLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    List<AnalysisLog> findByUserIdAndEndDateBetweenOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    AnalysisLog findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}

