package com.ssafy.fitmeet.domain.analysis.service;

import com.ssafy.fitmeet.domain.analysis.dto.AnalysisLogDto.WeeklyAiAnalysisLogResponse;

import java.time.LocalDate;
import java.util.List;

public interface AnalysisLogService {

    List<WeeklyAiAnalysisLogResponse> getLogs(Long userId);

    List<WeeklyAiAnalysisLogResponse> getLogsByDate(Long userId, LocalDate from, LocalDate to);

    void deleteLog(Long userId, Long logId);
}
