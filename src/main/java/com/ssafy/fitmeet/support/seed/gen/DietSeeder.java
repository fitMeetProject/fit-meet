package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.diet.dao.DietDao;
import com.ssafy.fitmeet.domain.diet.dao.DietInfoDao;
import com.ssafy.fitmeet.domain.diet.dao.DietScoreDao;
import com.ssafy.fitmeet.domain.diet.dao.UserDailyNutritionDao;
import com.ssafy.fitmeet.domain.diet.entity.Diet;
import com.ssafy.fitmeet.domain.diet.entity.DietInfo;
import com.ssafy.fitmeet.domain.diet.entity.DietScore;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.meal.dao.MealNutritionDao;
import com.ssafy.fitmeet.domain.meal.entity.MealNutrition;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DietSeeder {

    private final Faker faker;

    private final DietDao dietDao;
    private final DietInfoDao dietInfoDao;
    private final DietScoreDao dietScoreDao;
    private final UserDailyNutritionDao userDailyNutritionDao;

    private final MealNutritionDao mealNutritionDao;

    /**
     * Diet + DietInfo + DietScore 생성 후
     * UserDailyNutrition은 집계 쿼리로 upsert
     */
    @Transactional
    public List<Long> seedDiets(List<Long> userIds, List<Long> mealIds, int dietCount, int daysRange) {
        List<Long> dietIds = new ArrayList<>(dietCount);

        // daily upsert 대상 (userId, date)
        Set<UserDateKey> touched = new HashSet<>();

        for (int i = 0; i < dietCount; i++) {
            Long userId = pickOne(userIds);

            // 최근 daysRange일 내 임의 날짜/시간
            LocalDate date = LocalDate.now().minusDays(faker.number().numberBetween(0, daysRange));
            LocalTime time = LocalTime.of(
                    faker.number().numberBetween(6, 23),
                    faker.number().numberBetween(0, 60)
            );
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            String mealType = pickOne(List.of("A","B","C","D","E")); // 아침/점심/저녁/간식/야식

            // diet_info(음식들) 먼저 만들기 위해 meal을 2~4개 뽑는다
            int itemCnt = faker.number().numberBetween(2, 5);

            List<DietInfo> infos = new ArrayList<>(itemCnt);

            BigDecimal totalKcal = bd0();
            BigDecimal totalCarb = bd0();
            BigDecimal totalProtein = bd0();
            BigDecimal totalFat = bd0();
            BigDecimal totalSugar = bd0();
            BigDecimal totalSodium = bd0();

            for (int k = 0; k < itemCnt; k++) {
                Long mealId = pickOne(mealIds);
                MealNutrition base = mealNutritionDao.findByMealId(mealId);
                if (base == null) {
                    // 혹시 nutrition이 누락된 meal이 있다면 skip (정상 데이터면 거의 안 탐)
                    continue;
                }

                // 섭취량: 50~250g
                BigDecimal intakeGram = bd3(faker.number().randomDouble(0, 50, 250));

                // base_amount_g는 보통 100.000 (너희 시더에서 그렇게 넣음)
                BigDecimal baseG = nz(base.getBaseAmountG(), bd3(100));

                // 비율 = intake / base
                BigDecimal ratio = intakeGram.divide(baseG, 8, RoundingMode.HALF_UP);

                BigDecimal kcal = mul3(base.getKcal(), ratio);
                BigDecimal carb = mul3(base.getCarbohydrate(), ratio);
                BigDecimal protein = mul3(base.getProtein(), ratio);
                BigDecimal fat = mul3(base.getFat(), ratio);
                BigDecimal sugar = mul3(base.getSugar(), ratio);
                BigDecimal sodium = mul3(base.getSodium(), ratio);
                BigDecimal fiber = mul3(base.getDietaryFiber(), ratio);

                DietInfo di = new DietInfo();
                // dietId는 diet insert 후에 세팅
                di.setMealId(mealId);
                di.setFoodNmKr("SEED_FOOD_" + faker.food().dish()); // 실제 서비스에선 meal명 조인해서 넣어도 됨
                di.setFoodCode(null);
                di.setSourceType("PUBLIC_API"); // or MANUAL/MODEL
                di.setIntakeGram(intakeGram);

                di.setKcal(kcal);
                di.setCarbohydrate(carb);
                di.setProtein(protein);
                di.setFat(fat);
                di.setSugar(sugar);
                di.setSodium(sodium);
                di.setDietaryFiber(fiber);

                infos.add(di);

                totalKcal = totalKcal.add(kcal);
                totalCarb = totalCarb.add(carb);
                totalProtein = totalProtein.add(protein);
                totalFat = totalFat.add(fat);
                totalSugar = totalSugar.add(sugar);
                totalSodium = totalSodium.add(sodium);
            }

            // Diet 생성 (총합은 diet_info 합계와 동일)
            Diet diet = new Diet();
            diet.setUserId(userId);
            diet.setDate(dateTime);
            diet.setMealType(mealType);
            diet.setDescription(faker.bool().bool() ? faker.lorem().sentence(5) : null);
            diet.setImageUrl(null);
            diet.setSourceType("MANUAL");

            diet.setTotalKcal(bd3(totalKcal));
            diet.setTotalCarbohydrate(bd3(totalCarb));
            diet.setTotalProtein(bd3(totalProtein));
            diet.setTotalFat(bd3(totalFat));
            diet.setTotalSugar(bd3(totalSugar));
            diet.setTotalSodium(bd3(totalSodium));

            diet.setIsPublic(faker.number().numberBetween(0, 100) < 20); // 20% 공개

            dietDao.insert(diet); // useGeneratedKeys => diet.id
            Long dietId = diet.getId();
            dietIds.add(dietId);

            // diet_info insertBatch
            for (DietInfo di : infos) {
                di.setDietId(dietId);
            }
            if (!infos.isEmpty()) {
                dietInfoDao.insertBatch(infos);
            }

            // diet_score 1:1 생성
            DietScore ds = new DietScore();
            ds.setDietId(dietId);
            int score = faker.number().numberBetween(30, 101);
            ds.setScore(score);
            ds.setGrade(grade(score));
            ds.setFeedback(seedFeedback(score));
            dietScoreDao.insert(ds);

            // daily nutrition 집계 대상 기록
            touched.add(new UserDateKey(userId, date));
        }

        // user_daily_nutrition upsert (집계 쿼리 기반)
        upsertUserDailyNutrition(touched);

        return dietIds;
    }

    private void upsertUserDailyNutrition(Set<UserDateKey> touched) {
        for (UserDateKey key : touched) {
            UserDailyNutrition agg = userDailyNutritionDao.aggregateByUserAndDate(key.userId, key.date);
            if (agg == null) {
                // 해당 날짜 diet가 하나도 없는 경우
                userDailyNutritionDao.deleteByUserAndDate(key.userId, key.date);
                continue;
            }

            UserDailyNutrition existing = userDailyNutritionDao.findByUserAndDate(key.userId, key.date);
            if (existing == null) {
                userDailyNutritionDao.insert(agg);
            } else {
                userDailyNutritionDao.update(agg);
            }
        }
    }

    @Transactional
    public List<Long> seedDietsForUser(
            Long userId,
            List<Long> mealIds,
            int daysRange,
            int perDayMin,
            int perDayMax
    ) {
        List<Long> userIds = List.of(userId);
        List<Long> dietIds = new ArrayList<>();
        Set<UserDateKey> touched = new HashSet<>();

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(daysRange);

        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            int make = faker.number().numberBetween(perDayMin, perDayMax + 1);

            for (int i = 0; i < make; i++) {

                LocalTime time = LocalTime.of(
                        faker.number().numberBetween(6, 23),
                        faker.number().numberBetween(0, 60)
                );
                LocalDateTime dateTime = LocalDateTime.of(d, time);

                String mealType = pickOne(List.of("A","B","C","D","E"));

                int itemCnt = faker.number().numberBetween(2, 5);
                List<DietInfo> infos = new ArrayList<>(itemCnt);

                BigDecimal totalKcal = bd0();
                BigDecimal totalCarb = bd0();
                BigDecimal totalProtein = bd0();
                BigDecimal totalFat = bd0();
                BigDecimal totalSugar = bd0();
                BigDecimal totalSodium = bd0();

                for (int k = 0; k < itemCnt; k++) {
                    Long mealId = pickOne(mealIds);
                    MealNutrition base = mealNutritionDao.findByMealId(mealId);
                    if (base == null) continue;

                    BigDecimal intakeGram = bd3(faker.number().randomDouble(0, 50, 250));
                    BigDecimal baseG = nz(base.getBaseAmountG(), bd3(100));
                    BigDecimal ratio = intakeGram.divide(baseG, 8, RoundingMode.HALF_UP);

                    BigDecimal kcal = mul3(base.getKcal(), ratio);
                    BigDecimal carb = mul3(base.getCarbohydrate(), ratio);
                    BigDecimal protein = mul3(base.getProtein(), ratio);
                    BigDecimal fat = mul3(base.getFat(), ratio);
                    BigDecimal sugar = mul3(base.getSugar(), ratio);
                    BigDecimal sodium = mul3(base.getSodium(), ratio);
                    BigDecimal fiber = mul3(base.getDietaryFiber(), ratio);

                    DietInfo di = new DietInfo();
                    di.setMealId(mealId);
                    di.setFoodNmKr("DEMO_FOOD_" + faker.food().dish());
                    di.setFoodCode(null);
                    di.setSourceType("PUBLIC_API");
                    di.setIntakeGram(intakeGram);

                    di.setKcal(kcal);
                    di.setCarbohydrate(carb);
                    di.setProtein(protein);
                    di.setFat(fat);
                    di.setSugar(sugar);
                    di.setSodium(sodium);
                    di.setDietaryFiber(fiber);

                    infos.add(di);

                    totalKcal = totalKcal.add(kcal);
                    totalCarb = totalCarb.add(carb);
                    totalProtein = totalProtein.add(protein);
                    totalFat = totalFat.add(fat);
                    totalSugar = totalSugar.add(sugar);
                    totalSodium = totalSodium.add(sodium);
                }

                Diet diet = new Diet();
                diet.setUserId(userId);
                diet.setDate(dateTime);
                diet.setMealType(mealType);
                diet.setDescription(faker.bool().bool() ? faker.lorem().sentence(5) : "데모 식단 기록");
                diet.setImageUrl(null);
                diet.setSourceType("MANUAL");

                diet.setTotalKcal(bd3(totalKcal));
                diet.setTotalCarbohydrate(bd3(totalCarb));
                diet.setTotalProtein(bd3(totalProtein));
                diet.setTotalFat(bd3(totalFat));
                diet.setTotalSugar(bd3(totalSugar));
                diet.setTotalSodium(bd3(totalSodium));
                diet.setIsPublic(true); // 데모는 공개가 많아야 커뮤니티 연동 시연이 편함

                dietDao.insert(diet);
                Long dietId = diet.getId();
                dietIds.add(dietId);

                for (DietInfo di : infos) di.setDietId(dietId);
                if (!infos.isEmpty()) dietInfoDao.insertBatch(infos);

                DietScore ds = new DietScore();
                ds.setDietId(dietId);
                int score = faker.number().numberBetween(60, 96); // 데모는 너무 낮게 안 나오게
                ds.setScore(score);
                ds.setGrade(grade(score));
                ds.setFeedback(seedFeedback(score));
                dietScoreDao.insert(ds);

                touched.add(new UserDateKey(userId, d));
            }
        }

        upsertUserDailyNutrition(touched);
        return dietIds;
    }


    // ---------- helpers ----------

    private <T> T pickOne(List<T> list) {
        return list.get(faker.number().numberBetween(0, list.size()));
    }

    private String grade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    private String seedFeedback(int score) {
        if (score >= 90) return "균형이 매우 좋아요.";
        if (score >= 80) return "전반적으로 좋아요. 조금만 더 조절해봐요.";
        if (score >= 70) return "무난해요. 단백질/채소 비중을 늘려보세요.";
        if (score >= 60) return "개선 여지가 있어요. 당/나트륨을 줄여보세요.";
        return "식단 구성이 불균형해요. 간단한 목표부터 잡아보세요.";
    }

    private BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v == null ? def : v;
    }

    private BigDecimal bd0() {
        return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal bd3(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal bd3(BigDecimal v) {
        if (v == null) return bd0();
        return v.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal mul3(BigDecimal base, BigDecimal ratio) {
        if (base == null) return bd0();
        return base.multiply(ratio).setScale(3, RoundingMode.HALF_UP);
    }

    private record UserDateKey(Long userId, LocalDate date) {}
}
