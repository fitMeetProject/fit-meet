package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.meal.dao.MealDao;
import com.ssafy.fitmeet.domain.meal.dao.MealNutritionDao;
import com.ssafy.fitmeet.domain.meal.entity.Meal;
import com.ssafy.fitmeet.domain.meal.entity.MealNutrition;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class MealSeeder {

    private final Faker faker;
    private final MealDao mealDao;
    private final MealNutritionDao mealNutritionDao;

    public List<Long> seedMeals(int count) {
        List<Meal> meals = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            Meal m = new Meal();
            m.setFoodCd(String.format("FD%06d", i)); // uk_meal_food_cd 안전
            m.setFoodNmKr(fakeFoodName());
            m.setDbGrpCm("D");
            m.setDbGrpNm("음식");

            m.setFoodCat1Cd(null);
            m.setFoodCat1Nm(null);
            m.setFoodCat2Cd(null);
            m.setFoodCat2Nm(null);
            m.setFoodCat3Cd(null);
            m.setFoodCat3Nm(null);

            int serving = faker.number().numberBetween(50, 301);
            m.setServingSizeRaw(serving + "g");

            String sourceType = faker.bool().bool() ? "PUBLIC_API" : "CUSTOM";
            m.setSourceType(sourceType);

            m.setCategory(pickOne(List.of("밥류", "면류", "국/탕류", "샐러드", "디저트", "음료", "간편식")));
            m.setBrand(faker.bool().bool() ? faker.company().name() : null);
            m.setTags(makeTags());

            m.setUpdateDate(LocalDate.now().minusDays(faker.number().numberBetween(0, 365)));

            meals.add(m);
        }

        mealDao.insertMany(meals);

        List<String> foodCds = meals.stream().map(Meal::getFoodCd).collect(Collectors.toList());
        return mealDao.findIdsByFoodCdIn(foodCds);
    }

    public void seedMealNutrition(List<Long> mealIds) {
        List<MealNutrition> list = new ArrayList<>(mealIds.size());

        for (Long mealId : mealIds) {
            int baseAmount = 100; // 100g 기준

            double carb = faker.number().randomDouble(3, 0, 80);
            double protein = faker.number().randomDouble(3, 0, 40);
            double fat = faker.number().randomDouble(3, 0, 35);

            double sugar = Math.min(carb, faker.number().randomDouble(3, 0, 30));
            double sodium = faker.number().randomDouble(3, 0, 1500);
            double fiber = faker.number().randomDouble(3, 0, 15);

            double kcal = carb * 4 + protein * 4 + fat * 9;

            MealNutrition n = MealNutrition.builder()
                    .mealId(mealId)
                    .baseAmountG(bd3(baseAmount))
                    .carbohydrate(bd3(carb))
                    .protein(bd3(protein))
                    .fat(bd3(fat))
                    .sugar(bd3(sugar))
                    .sodium(bd3(sodium))
                    .dietaryFiber(bd3(fiber))
                    .kcal(bd3(kcal))
                    .build();

            list.add(n);
        }

        mealNutritionDao.insertMany(list);
    }

    private BigDecimal bd3(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal bd3(int v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }

    private String fakeFoodName() {
        String base = pickOne(List.of("닭가슴살", "현미밥", "김치찌개", "비빔밥", "샐러드", "요거트", "스테이크", "파스타", "라면", "연어덮밥", "계란말이"));
        String suffix = pickOne(List.of("", " (소)", " (대)", " 도시락", " 스페셜", " 세트"));
        return base + suffix;
    }

    private String makeTags() {
        List<String> pool = new ArrayList<>(List.of("고단백", "저탄수", "저지방", "벌크업", "다이어트", "클린", "간편식", "비건", "무가당"));
        Collections.shuffle(pool, new Random(faker.random().nextInt()));
        int cnt = faker.number().numberBetween(1, 4);
        return String.join(",", pool.subList(0, cnt));
    }

    private <T> T pickOne(List<T> list) {
        return list.get(faker.number().numberBetween(0, list.size()));
    }
}
