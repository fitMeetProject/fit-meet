package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.user.dao.UserBodyInfoDao;
import com.ssafy.fitmeet.domain.user.dao.UserDao;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserSeeder {

    private final Faker faker;
    private final UserDao userDao;
    private final UserBodyInfoDao userBodyInfoDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * User 생성
     */
    public List<Long> seedUsers(int count) {
        List<Long> userIds = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            String email = String.format(
                    "seed%04d_%s@fitmeet.dev",
                    i,
                    faker.random().hex(6)
            );

            User u = new User();
            u.setEmail(email);
            u.setPassword(passwordEncoder.encode("Password123!"));
            u.setName(faker.name().fullName());
            u.setNickname("seed_" + faker.name().username() + "_" + i);

            u.setStatus("ACTIVE");
            u.setRole("ROLE_USER");
            u.setProfileImageUrl(null);

            u.setEmailVerified(true);
            u.setLastLoginAt(null);
            u.setPasswordChangedAt(
                    LocalDateTime.now().minusDays(faker.number().numberBetween(1, 60))
            );

            userDao.insertUser(u);
            userIds.add(u.getId());
        }

        return userIds;
    }

    /**
     * 프로필 이미지 URL 업데이트(일부 유저만)
     * @param userIds 대상 유저들
     * @param profileUrls provision된 프로필 이미지 URL 풀
     * @param ratePercent 0~100 (예: 60이면 60% 확률로 프로필사진 부여)
     */
    public void seedProfileImages(List<Long> userIds, List<String> profileUrls, int ratePercent) {
        if (userIds == null || userIds.isEmpty()) return;
        if (profileUrls == null || profileUrls.isEmpty()) return;

        for (Long userId : userIds) {
            if (faker.number().numberBetween(0, 100) >= ratePercent) continue;

            String url = profileUrls.get(faker.number().numberBetween(0, profileUrls.size()));
            userDao.updateProfileImageUrl(userId, url);
        }
    }

    /**
     * 데모 유저는 100% 프로필 사진 지정 (항상 눈에 띄게)
     */
    public void seedProfileImageForUser(Long userId, List<String> profileUrls) {
        if (userId == null) return;
        if (profileUrls == null || profileUrls.isEmpty()) return;

        String url = profileUrls.get(faker.number().numberBetween(0, profileUrls.size()));
        userDao.updateProfileImageUrl(userId, url);
    }

    /**
     * UserBodyInfo 생성 (일부만)
     */
    public void seedBodyInfos(List<Long> userIds, int bodyInfoRate) {
        for (Long userId : userIds) {
            if (faker.number().numberBetween(1, 101) > bodyInfoRate) continue;

            UserBodyInfo info = new UserBodyInfo();
            info.setUserId(userId);

            Double height = round2(faker.number().randomDouble(2, 150, 195));
            Double weight = round2(faker.number().randomDouble(2, 45, 110));
            Double target = round2(Math.max(40.0, weight - faker.number().numberBetween(0, 10)));

            info.setHeightCm(height);
            info.setWeightKg(weight);
            info.setTargetWeightKg(target);

            info.setGender(
                    switch (faker.number().numberBetween(0, 3)) {
                        case 0 -> "MALE";
                        case 1 -> "FEMALE";
                        default -> "OTHER";
                    }
            );

            info.setBirthDate(
                    LocalDate.now()
                            .minusYears(faker.number().numberBetween(18, 45))
                            .minusDays(faker.number().numberBetween(0, 365))
            );

            String activityLevel =
                    switch (faker.number().numberBetween(0, 4)) {
                        case 0 -> "NONE";
                        case 1 -> "LOW";
                        case 2 -> "MID";
                        default -> "HIGH";
                    };

            info.setActivityLevel(activityLevel);

            int bmr = 1200 + faker.number().numberBetween(0, 600);
            int dailyKcal = bmr + switch (activityLevel) {
                case "NONE" -> 200;
                case "LOW" -> 400;
                case "MID" -> 600;
                default -> 800;
            };

            info.setBmr(bmr);
            info.setDailyKcal(dailyKcal);

            userBodyInfoDao.insertBodyInfo(info);
        }
    }

    private Double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
