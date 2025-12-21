package com.ssafy.fitmeet.domain.analysis.service;

import com.ssafy.fitmeet.domain.analysis.dao.AnalysisLogDao;
import com.ssafy.fitmeet.domain.analysis.dto.AnalysisLogDto.WeeklyAiAnalysisLogResponse;
import com.ssafy.fitmeet.domain.analysis.entity.AnalysisLog;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.util.WeeklyAiResultParser;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisLogServiceImpl implements AnalysisLogService {

    private final AnalysisLogDao analysisLogDao;
    private final WeeklyAiResultParser parser;

    @Override
    public List<WeeklyAiAnalysisLogResponse> getLogs(Long userId) {
        return analysisLogDao.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<WeeklyAiAnalysisLogResponse> getLogsByDate(Long userId, LocalDate from, LocalDate to) {
        return analysisLogDao.findByUserIdAndEndDateBetweenOrderByCreatedAtDesc(userId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteLog(Long userId, Long logId) {
        int deleted = analysisLogDao.deleteByIdAndUserId(logId, userId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.DB_DATA_NOT_FOUND);
        }
    }

    private WeeklyAiAnalysisLogResponse toResponse(AnalysisLog row) {
        return new WeeklyAiAnalysisLogResponse(
                row.getId(),
                row.getStartDate(),
                row.getEndDate(),
                row.getWeeklyScore(),
                parser.parseOrNull(row.getResponseJson()),
                row.getStatus(),
                row.getErrorMessage(),
                row.getCreatedAt()
        );
    }
}
