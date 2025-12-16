package com.ssafy.fitmeet.domain.dashboard.service;

import com.ssafy.fitmeet.domain.dashboard.dao.UserDailyScoreDao;
import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import com.ssafy.fitmeet.domain.dashboard.service.score.DashboardDailyScoreCalculator;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.diet.service.UserDailyNutritionService;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.service.UserDailyActivityService;
import com.ssafy.fitmeet.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDailyScoreServiceImpl implements UserDailyScoreService {

    private final UserDailyScoreDao userDailyScoreDao;

    private final UserDailyNutritionService userDailyNutritionService;
    private final UserDailyActivityService userDailyActivityService;
    private final UserService userService;

    @Override
    @Transactional
    public UserDailyScore refreshDailyScore(Long userId, LocalDate date) {

        // 1) 하루 영양소 집계(없으면 null 가능) - null이면 0으로 계산됨
        UserDailyNutrition nut = userDailyNutritionService.getDaily(userId, date);

        // 2) 신체정보(없을 수 있음)
        UserBodyInfo bodyInfo = userService.findByUserId(userId);

        // 3) 활동 기록(없을 수 있음)
        boolean dietLogged = userDailyActivityService.isDietLogged(userId, date);
        boolean workoutLogged = userDailyActivityService.isWorkoutLogged(userId, date);

        // 4) 점수 계산
        var result = DashboardDailyScoreCalculator.calculate(nut, bodyInfo, dietLogged, workoutLogged);

        // 5) 스냅샷 필드 구성
        Integer dailyKcalSnapshot = (bodyInfo != null) ? bodyInfo.getDailyKcal() : null;
        Double totalKcalSnapshot = (nut != null && nut.getTotalKcal() != null) ? nut.getTotalKcal().doubleValue() : 0.0;

        UserDailyScore score = UserDailyScore.builder()
                .userId(userId)
                .date(date)
                .score(result.score())
                .grade(result.grade())
                .feedback(result.feedback())
                .dailyKcal(dailyKcalSnapshot)
                .totalKcal(totalKcalSnapshot)
                .build();

        // 6) upsert
        userDailyScoreDao.upsert(score);

        // 7) 반환은 DB 재조회해도 되고, 지금 객체 그대로 반환해도 무방
        //    (id가 필요하면 재조회 권장)
        return userDailyScoreDao.findByUserAndDate(userId, date);
    }

    @Override
    @Transactional
    public List<UserDailyScore> refreshWeek(Long userId, LocalDate endDate) {
        LocalDate start = endDate.minusDays(6);

        // 요구사항: "호출될 때마다 리프레시"
        List<UserDailyScore> res = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(endDate)) {
            res.add(refreshDailyScore(userId, d));
            d = d.plusDays(1);
        }
        return res;
    }

    @Override
    @Transactional
    public List<UserDailyScore> refreshMonth(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<UserDailyScore> res = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            res.add(refreshDailyScore(userId, d));
            d = d.plusDays(1);
        }
        return res;
    }
}
