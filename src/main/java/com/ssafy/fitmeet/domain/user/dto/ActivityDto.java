package com.ssafy.fitmeet.domain.user.dto;

import java.time.LocalDate;
import java.util.List;

public class ActivityDto {

    public record DietActivityDay(
            LocalDate date,
            boolean dietLogged
    ) {}

    public record DietStreakInfo(
            int streakCount,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    /**
     * 기간 + 스트릭 한번에 보고 싶을 때
     */
    public record DietActivityRangeResponse(
            LocalDate fromDate,
            LocalDate toDate,
            List<DietActivityDay> days
    ) {}
}
