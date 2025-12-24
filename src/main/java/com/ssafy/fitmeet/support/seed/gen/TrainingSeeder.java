package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoHistoryDao;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoHistory;
import com.ssafy.fitmeet.domain.user.dao.UserDailyActivityDao;
import com.ssafy.fitmeet.domain.user.entity.UserDailyActivity;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class TrainingSeeder {

    private final Faker faker;

    private final TrainingVideoHistoryDao historyDao;
    private final UserDailyActivityDao userDailyActivityDao;

    /**
     * 운동 기록 시드
     * - daysRange: 최근 N일 범위
     * - perUserDaysRate: 해당 날짜에 운동할 확률(0~100)
     * - perDayMin~Max: 운동한 날에 기록할 영상 개수
     */
    @Transactional
    public void seedWorkoutHistory(List<Long> userIds,
                                   List<Long> videoIds,
                                   int daysRange,
                                   int perUserDaysRate,
                                   int perDayMin,
                                   int perDayMax) {

        if (videoIds == null || videoIds.isEmpty()) return;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(daysRange);

        for (Long userId : userIds) {
            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {

                // 확률적으로 "운동한 날" 만들기
                if (faker.number().numberBetween(1, 101) > perUserDaysRate) continue;

                int cnt = faker.number().numberBetween(perDayMin, perDayMax + 1);

                // 같은 날 같은 video 중복 방지(UNIQUE 제약 대응)
                Set<Long> picked = pickDistinct(videoIds, cnt);

                boolean anyCompleted = false;

                for (Long videoId : picked) {
                    // completed 대부분 true, 일부 false(기록은 남되 미완료 느낌)
                    int completed = faker.number().numberBetween(0, 100) < 85? 1: 0;

                    TrainingVideoHistory row = new TrainingVideoHistory();
                    row.setUserId(userId);
                    row.setVideoId(videoId);
                    row.setActivityDate(d);
                    row.setCompleted(completed);
                    row.setDurationSec(faker.number().numberBetween(300, 3600)); // 5~60분
                    row.setMemo(faker.bool().bool() ? faker.lorem().sentence(4) : null);

                    // insert 시 created_at/updated_at은 NOW()라 엔티티 값 필요 없음
                    historyDao.insertHistory(row);

                    if (completed==1) anyCompleted = true;
                }

                // user_daily_activity.workout_logged upsert (운동 완료가 1개라도 있으면 true)
                if (anyCompleted) {
                    upsertWorkoutLogged(userId, d);
                }
            }
        }
    }

    private void upsertWorkoutLogged(Long userId, LocalDate date) {
        UserDailyActivity act = userDailyActivityDao.findByUserAndDate(userId, date);

        if (act == null) {
            act = new UserDailyActivity();
            act.setUserId(userId);
            act.setDate(date);
            act.setDietLogged(false);
            act.setWorkoutLogged(true);
            act.setChallengeDone(false);
            userDailyActivityDao.insert(act);
            return;
        }

        act.setWorkoutLogged(true);
        userDailyActivityDao.update(act);
    }


    private Set<Long> pickDistinct(List<Long> pool, int count) {
        int n = Math.min(count, pool.size());
        Set<Long> picked = new LinkedHashSet<>(n);

        // 단순 랜덤 추출
        while (picked.size() < n) {
            Long v = pool.get(faker.number().numberBetween(0, pool.size()));
            picked.add(v);
        }
        return picked;
    }
}
