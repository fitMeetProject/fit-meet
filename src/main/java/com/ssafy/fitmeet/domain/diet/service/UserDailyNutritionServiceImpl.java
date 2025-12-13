package com.ssafy.fitmeet.domain.diet.service;

import com.ssafy.fitmeet.domain.diet.dao.UserDailyNutritionDao;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDailyNutritionServiceImpl implements UserDailyNutritionService {

    private final UserDailyNutritionDao userDailyNutritionDao;

    /**
     * 특정 유저 + 날짜에 대해 diet/diet_score 기준으로 다시 집계해서
     * user_daily_nutrition 테이블에 반영.
     *
     * - 해당 날짜에 diet가 하나도 없으면 row 삭제
     * - 하나라도 있으면 insert or update
     */
    @Override
    @Transactional
    public void recalcDailyNutrition(Long userId, LocalDate date) {

        // 1. diet + diet_score 기반 집계 (없으면 null)
        UserDailyNutrition aggregated = userDailyNutritionDao.aggregateByUserAndDate(userId, date);

        if (aggregated == null) {
            // 해당 날짜에 더 이상 식단이 없으면 요약 row 삭제
            userDailyNutritionDao.deleteByUserAndDate(userId, date);
            return;
        }

        // 2. 기존 row 있는지 확인
        UserDailyNutrition existing = userDailyNutritionDao.findByUserAndDate(userId, date);

        if (existing == null) {
            userDailyNutritionDao.insert(aggregated);
        } else {
            userDailyNutritionDao.update(aggregated);
        }
    }

    /**
     * 특정 유저 + 날짜 요약 조회 (없으면 null)
     */
    @Override
    @Transactional(readOnly = true)
    public UserDailyNutrition getDaily(Long userId, LocalDate date) {
        return userDailyNutritionDao.findByUserAndDate(userId, date);
    }

    /**
     * 특정 기간 [startDate ~ endDate] 요약 리스트 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDailyNutrition> getRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return userDailyNutritionDao.findRange(userId, startDate, endDate);
    }
}
