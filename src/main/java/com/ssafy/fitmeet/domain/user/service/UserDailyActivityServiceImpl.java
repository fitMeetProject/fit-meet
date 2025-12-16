package com.ssafy.fitmeet.domain.user.service;

import com.ssafy.fitmeet.domain.user.dao.UserDailyActivityDao;
import com.ssafy.fitmeet.domain.user.dto.ActivityDto.DietActivityDay;
import com.ssafy.fitmeet.domain.user.dto.ActivityDto.DietStreakInfo;
import com.ssafy.fitmeet.domain.user.entity.UserDailyActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserDailyActivityServiceImpl implements UserDailyActivityService {

    private final UserDailyActivityDao userDailyActivityDao;

    /**
     * 해당 날짜에 식단 기록이 존재하게 되었을 때 호출
     * - diet 생성 시 호출
     * - diet 수정으로 날짜가 변경될 때, 새 날짜에 대해 호출
     */
    @Override
    @Transactional
    public void markDietLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);

        if (activity == null) {
            activity = UserDailyActivity.builder()
                    .userId(userId)
                    .date(date)
                    .dietLogged(true)
                    .workoutLogged(false)
                    .challengeDone(false)
                    .build();
            userDailyActivityDao.insert(activity);
        } else {
            if (Boolean.TRUE.equals(activity.getDietLogged())) {
                return;
            }
            activity.setDietLogged(true);
            userDailyActivityDao.update(activity);
        }
    }

    /**
     * 해당 날짜에 "더 이상 식단 기록이 없을 때" 호출
     * - 마지막 diet 삭제 후 호출
     */
    @Override
    @Transactional
    public void unmarkDietLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);

        if (activity == null) return;

        activity.setDietLogged(false);
        userDailyActivityDao.update(activity);
    }

    @Override
    @Transactional
    public void markWorkoutLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);

        if (activity == null) {
            activity = UserDailyActivity.builder()
                    .userId(userId)
                    .date(date)
                    .dietLogged(false)
                    .workoutLogged(true)
                    .challengeDone(false)
                    .build();
            userDailyActivityDao.insert(activity);
        } else {
            if (Boolean.TRUE.equals(activity.getWorkoutLogged())) {
                return;
            }
            activity.setWorkoutLogged(true);
            userDailyActivityDao.update(activity);
        }
    }

    @Override
    @Transactional
    public void unmarkWorkoutLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);
        if (activity == null) return;

        activity.setWorkoutLogged(false);
        userDailyActivityDao.update(activity);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isDietLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);
        return activity != null && Boolean.TRUE.equals(activity.getDietLogged());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkoutLogged(Long userId, LocalDate date) {
        UserDailyActivity activity = userDailyActivityDao.findByUserAndDate(userId, date);
        return activity != null && Boolean.TRUE.equals(activity.getWorkoutLogged());
    }

    /**
     * 캘린더/스트릭 뷰용:
     * - fromDate ~ toDate 사이의 모든 날짜에 대해 dietLogged 여부를 내려줌
     * - row가 없는 날은 dietLogged = false 로 만들어서 채워줌
     */
    @Override
    @Transactional(readOnly = true)
    public List<DietActivityDay> getDietActivityRange(Long userId, LocalDate fromDate, LocalDate toDate) {

        List<UserDailyActivity> rows = userDailyActivityDao.findBetweenDates(userId, fromDate, toDate);

        Map<LocalDate, UserDailyActivity> map = new HashMap<>();
        for (UserDailyActivity a : rows) {
            map.put(a.getDate(), a);
        }

        List<DietActivityDay> result = new ArrayList<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            UserDailyActivity a = map.get(cursor);
            boolean logged = (a != null && Boolean.TRUE.equals(a.getDietLogged()));

            result.add(new DietActivityDay(cursor, logged));
            cursor = cursor.plusDays(1);
        }

        return result;
    }

    /**
     * 현재 식단 스트릭 계산
     * - 기준: 오늘부터 뒤로 연속해서 dietLogged = true 인 날의 수
     * - 중간에 하나라도 false/데이터없음이면 거기서 끊기고, 그 이후는 새로운 스트릭
     */
    @Override
    @Transactional(readOnly = true)
    public DietStreakInfo getCurrentDietStreak(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(30);

        List<DietActivityDay> days = getDietActivityRange(userId, fromDate, today);

        int streak = 0;
        LocalDate start = null;
        LocalDate end = null;

        for (int i = days.size() - 1; i >= 0; i--) {
            DietActivityDay d = days.get(i);

            if (d.dietLogged()) {
                streak++;
                if (end == null) {
                    end = d.date(); // 첫 번째로 만난(가장 최근) true 날짜
                }
                start = d.date(); // 계속 갱신되면서 결국 가장 오래된 날짜가 start
            } else {
                // 오늘부터 보는 기준에서 처음으로 false를 만나면 거기서 끊김
                if (streak > 0) break;
                // streak == 0이면 아직 “최근에 아무것도 안한 상태”라서 그대로 계속 진행
            }
        }

        if (streak == 0) {
            return new DietStreakInfo(0, null, null);
        }
        return new DietStreakInfo(streak, start, end);
    }
}
