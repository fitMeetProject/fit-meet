package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoHistoryDao;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoHistory;
import com.ssafy.fitmeet.domain.user.dao.UserDailyActivityDao;
import com.ssafy.fitmeet.domain.user.entity.UserDailyActivity;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrainingSeeder {

    private final Faker faker;

    private final TrainingVideoHistoryDao historyDao;
    private final UserDailyActivityDao userDailyActivityDao;

    @Transactional
    public void seedWorkoutHistory(
            List<Long> userIds,
            List<Long> videoIds,
            int daysRange,
            int perUserDaysRate,
            int perDayMin,
            int perDayMax
    ) {

        if (videoIds == null || videoIds.isEmpty()) return;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(daysRange);

        for (Long userId : userIds) {
            for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {

                if (faker.number().numberBetween(1, 101) > perUserDaysRate) continue;

                int cnt = faker.number().numberBetween(perDayMin, perDayMax + 1);
                Set<Long> picked = pickDistinct(videoIds, cnt);

                boolean anyCompleted = false;

                for (Long videoId : picked) {

                    int completed = faker.number().numberBetween(0, 100) < 85 ? 1 : 0;

                    LocalDateTime activityAt = d.atTime(
                            faker.number().numberBetween(6, 23),
                            faker.number().numberBetween(0, 60)
                    );

                    TrainingVideoHistory row = TrainingVideoHistory.builder()
                            .userId(userId)
                            .videoId(videoId)
                            .activityDate(d)
                            .completed(completed)
                            .durationSec(faker.number().numberBetween(300, 3600))
                            .memo(faker.bool().bool() ? faker.lorem().sentence(4) : null)
                            .createdAt(activityAt)
                            .updatedAt(activityAt)
                            .build();

                    historyDao.insertHistory(row);

                    if (completed == 1) anyCompleted = true;
                }

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
        Set<Long> picked = new LinkedHashSet<>();
        while (picked.size() < Math.min(count, pool.size())) {
            picked.add(pool.get(faker.number().numberBetween(0, pool.size())));
        }
        return picked;
    }
}

