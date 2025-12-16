package com.ssafy.fitmeet.domain.diet.service;

import com.ssafy.fitmeet.domain.diet.dao.DietDao;
import com.ssafy.fitmeet.domain.diet.dao.DietInfoDao;
import com.ssafy.fitmeet.domain.diet.dao.DietScoreDao;
import com.ssafy.fitmeet.domain.diet.dto.DietDto.*;
import com.ssafy.fitmeet.domain.diet.entity.Diet;
import com.ssafy.fitmeet.domain.diet.entity.DietInfo;
import com.ssafy.fitmeet.domain.diet.entity.DietScore;
import com.ssafy.fitmeet.domain.meal.entity.Meal;
import com.ssafy.fitmeet.domain.meal.entity.MealNutrition;
import com.ssafy.fitmeet.domain.meal.service.MealService;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.service.UserDailyActivityService;
import com.ssafy.fitmeet.domain.user.service.UserService;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DietServiceImpl implements DietService {

    private final DietDao dietDao;
    private final DietInfoDao dietInfoDao;
    private final DietScoreDao dietScoreDao;
    private final MealService mealService;
    private final UserService userService;

    private final UserDailyNutritionService userDailyNutritionService;
    private final UserDailyActivityService userDailyActivityService;

    private BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * DietInfo 리스트 기준으로 요약 영양소 합산 후 Diet 갱신
     */
    private void applyTotalNutritionAndUpdateDiet(Diet diet, List<DietInfo> dietInfos) {

        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalCarbo = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalSugar = BigDecimal.ZERO;
        BigDecimal totalSodium = BigDecimal.ZERO;

        for (DietInfo info : dietInfos) {
            if (info.getKcal() != null) totalKcal = totalKcal.add(info.getKcal());
            if (info.getCarbohydrate() != null) totalCarbo = totalCarbo.add(info.getCarbohydrate());
            if (info.getProtein() != null) totalProtein= totalProtein.add(info.getProtein());
            if (info.getFat() != null) totalFat = totalFat.add(info.getFat());
            if (info.getSugar() != null) totalSugar = totalSugar.add(info.getSugar());
            if (info.getSodium() != null) totalSodium = totalSodium.add(info.getSodium());
        }

        diet.setTotalKcal(totalKcal);
        diet.setTotalCarbohydrate(totalCarbo);
        diet.setTotalProtein(totalProtein);
        diet.setTotalFat(totalFat);
        diet.setTotalSugar(totalSugar);
        diet.setTotalSodium(totalSodium);
        diet.setUpdatedAt(LocalDateTime.now());

        dietDao.update(diet);
    }

    @Override
    @Transactional
    public Long createDiet(Long userId, DietCreateRequest request) {

        String dietSourceType = (request.sourceType() != null)
                ? request.sourceType()
                : "MANUAL";

        Diet diet = Diet.builder()
                .userId(userId)
                .date(request.date())
                .mealType(request.mealType())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .sourceType(dietSourceType)
                .totalKcal(BigDecimal.ZERO)
                .totalCarbohydrate(BigDecimal.ZERO)
                .totalProtein(BigDecimal.ZERO)
                .totalFat(BigDecimal.ZERO)
                .totalSugar(BigDecimal.ZERO)
                .totalSodium(BigDecimal.ZERO)
                .isPublic(Boolean.TRUE.equals(request.isPublic()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        dietDao.insert(diet);

        List<DietInfo> dietInfos = new ArrayList<>();

        if (request.foods() != null) {
            for (DietFoodRequest item : request.foods()) {

                String foodSourceType = (item.sourceType() != null) ? item.sourceType() : dietSourceType;

                DietInfo info;
                if ("MANUAL".equalsIgnoreCase(foodSourceType)) {
                    info = buildManualDietInfo(diet.getId(), item);
                } else {
                    info = buildAutoDietInfo(diet.getId(), item, foodSourceType);
                }

                dietInfos.add(info);
            }
        }

        if (!dietInfos.isEmpty()) {
            dietInfoDao.insertBatch(dietInfos);
        }

        applyTotalNutritionAndUpdateDiet(diet, dietInfos);
        recalcAndUpsertDietScore(userId, diet.getId());

        // 하루 영양소 집계 업데이트
        LocalDate localDate = diet.getDate().toLocalDate();
        userDailyNutritionService.recalcDailyNutrition(userId, localDate);

        // 해당 날짜 활동 기록 (식단 기록 여부)
        userDailyActivityService.markDietLogged(userId, localDate);

        return diet.getId();
    }

    /**
     * MANUAL: 사용자가 영양소까지 직접 입력한 경우
     */
    private DietInfo buildManualDietInfo(Long dietId, DietFoodRequest item) {

        BigDecimal kcal = zero(item.kcal());
        BigDecimal carbohydrate = zero(item.carbohydrate());
        BigDecimal protein = zero(item.protein());
        BigDecimal fat = zero(item.fat());
        BigDecimal sugar = zero(item.sugar());
        BigDecimal sodium = zero(item.sodium());
        BigDecimal dietaryFiber = zero(item.dietaryFiber());

        return DietInfo.builder()
                .dietId(dietId)
                .mealId(null)
                .foodNmKr(item.foodNmKr())
                .foodCode(item.foodCode())
                .sourceType("MANUAL")
                .intakeGram(item.intakeGram())
                .kcal(kcal)
                .carbohydrate(carbohydrate)
                .protein(protein)
                .fat(fat)
                .sugar(sugar)
                .sodium(sodium)
                .dietaryFiber(dietaryFiber)
                .build();
    }

    /**
     * API / IMAGE: 공공데이터 or 이미지 기반, foodCode + 섭취량만 들어옴
     */
    private DietInfo buildAutoDietInfo(Long dietId, DietFoodRequest item, String foodSourceType) {

        if (item.foodCode() == null || item.foodCode().isBlank()) {
            throw new CustomException(ErrorCode.DIET_FOOD_CODE_REQUIRED);
        }

        // 공공데이터/마스터에서 음식 + 기준 영양소 조회
        Meal meal = mealService.getOrFetchMealByFoodCd(item.foodCode());
        MealNutrition nutrition = mealService.getOrFetchNutritionByFoodCd(item.foodCode());

        BigDecimal intakeGram = item.intakeGram();
        BigDecimal baseAmount = nutrition.getBaseAmountG(); // 보통 100g 기준

        BigDecimal kcal = mealService.calculateScaled(baseAmount, nutrition.getKcal(), intakeGram);
        BigDecimal carbohydrate = mealService.calculateScaled(baseAmount, nutrition.getCarbohydrate(), intakeGram);
        BigDecimal protein = mealService.calculateScaled(baseAmount, nutrition.getProtein(), intakeGram);
        BigDecimal fat = mealService.calculateScaled(baseAmount, nutrition.getFat(), intakeGram);
        BigDecimal sugar = mealService.calculateScaled(baseAmount, nutrition.getSugar(), intakeGram);
        BigDecimal sodium = mealService.calculateScaled(baseAmount, nutrition.getSodium(), intakeGram);
        BigDecimal dietaryFiber = mealService.calculateScaled(baseAmount, nutrition.getDietaryFiber(), intakeGram);

        return DietInfo.builder()
                .dietId(dietId)
                .mealId(meal.getId())
                .foodNmKr(meal.getFoodNmKr())
                .foodCode(meal.getFoodCd())
                .sourceType(foodSourceType) // API / IMAGE
                .intakeGram(intakeGram)
                .kcal(zero(kcal))
                .carbohydrate(zero(carbohydrate))
                .protein(zero(protein))
                .fat(zero(fat))
                .sugar(zero(sugar))
                .sodium(zero(sodium))
                .dietaryFiber(zero(dietaryFiber))
                .build();
    }

    @Override
    @Transactional
    public void updateDiet(Long userId, Long dietId, DietUpdateRequest request) {

        Diet diet = dietDao.findById(dietId);
        if (diet == null) {
            throw new CustomException(ErrorCode.DIET_NOT_FOUND);
        }
        if (!diet.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }


        LocalDate oldDate = diet.getDate().toLocalDate();
        String dietSourceType = (request.sourceType() != null) ? request.sourceType() : diet.getSourceType();

        if (request.mealType() != null) {
            diet.setMealType(request.mealType());
        }
        diet.setDescription(request.description());
        diet.setImageUrl(request.imageUrl());
        diet.setSourceType(dietSourceType);
        diet.setIsPublic(Boolean.TRUE.equals(request.isPublic()));
        diet.setUpdatedAt(LocalDateTime.now());

        dietInfoDao.deleteByDietId(dietId);

        List<DietInfo> dietInfos = new ArrayList<>();

        if (request.foods() != null) {
            for (DietFoodRequest item : request.foods()) {

                String foodSourceType = (item.sourceType() != null) ? item.sourceType() : dietSourceType;

                DietInfo info;
                if ("MANUAL".equalsIgnoreCase(foodSourceType)) {
                    info = buildManualDietInfo(dietId, item);
                } else {
                    info = buildAutoDietInfo(dietId, item, foodSourceType);
                }

                dietInfos.add(info);
            }
        }

        if (!dietInfos.isEmpty()) {
            dietInfoDao.insertBatch(dietInfos);
        }

        applyTotalNutritionAndUpdateDiet(diet, dietInfos);
        recalcAndUpsertDietScore(userId, dietId);

        LocalDate newDate = diet.getDate().toLocalDate();

        // 날짜가 변경된 경우, 양쪽 날짜 모두 재계산
        if (!oldDate.equals(newDate)) {
            userDailyNutritionService.recalcDailyNutrition(userId, oldDate);
        }

        userDailyNutritionService.recalcDailyNutrition(userId, newDate);

        if (!oldDate.equals(newDate)) {
            // oldDate에서 해당 날짜 diet가 더 없으면 0으로
            List<Diet> remain = dietDao.findByUserAndLocalDate(userId, oldDate);
            if (remain == null || remain.isEmpty()) {
                userDailyActivityService.unmarkDietLogged(userId, oldDate);
            }
        }

        // newDate 기준으로는 무조건 1
        userDailyActivityService.markDietLogged(userId, newDate);
    }

    @Override
    @Transactional
    public Diet createDietWithInfos(Diet diet, List<DietInfo> dietInfos) {

        dietDao.insert(diet);

        for (DietInfo info : dietInfos) {
            info.setDietId(diet.getId());
        }
        if (!dietInfos.isEmpty()) {
            dietInfoDao.insertBatch(dietInfos);
        }

        applyTotalNutritionAndUpdateDiet(diet, dietInfos);

        return diet;
    }

    @Override
    @Transactional
    public void updateDietWithInfos(Diet diet, List<DietInfo> dietInfos) {

        dietInfoDao.deleteByDietId(diet.getId());

        for (DietInfo info : dietInfos) {
            info.setDietId(diet.getId());
        }
        if (!dietInfos.isEmpty()) {
            dietInfoDao.insertBatch(dietInfos);
        }

        applyTotalNutritionAndUpdateDiet(diet, dietInfos);
    }

    @Override
    public Diet getDiet(Long dietId) {
        Diet diet = dietDao.findById(dietId);
        if (diet == null) {
            throw new CustomException(ErrorCode.DIET_NOT_FOUND);
        }
        return diet;
    }

    @Override
    public List<DietInfo> getDietInfos(Long dietId) {
        return dietInfoDao.findByDietId(dietId);
    }

    @Override
    public List<Diet> getDietsByUserAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return dietDao.findByUserAndDateRange(userId, startDate, endDate);
    }

    @Override
    public List<Diet> getDietsByUserAndLocalDate(Long userId, LocalDate localDate) {
        return dietDao.findByUserAndLocalDate(userId, localDate);
    }

    @Override
    @Transactional
    public void deleteDiet(Long dietId) {
        Diet diet = dietDao.findById(dietId);
        if (diet == null) {
            throw new CustomException(ErrorCode.DIET_NOT_FOUND);
        }

        Long userId = diet.getUserId();
        LocalDate date = diet.getDate().toLocalDate();

        dietDao.softDelete(dietId);
        dietScoreDao.deleteByDietId(dietId);

        // 하루 영양소 집계 재계산
        userDailyNutritionService.recalcDailyNutrition(userId, date);

        // 해당 날짜에 남은 diet 여부 체크 → 활동 로그도 조정
        List<Diet> remain = dietDao.findByUserAndLocalDate(userId, date);
        if (remain == null || remain.isEmpty()) {
            userDailyActivityService.unmarkDietLogged(userId, date);
        }
    }

    @Override
    @Transactional
    public DietScore saveOrUpdateDietScore(DietScore dietScore) {
        DietScore existing = dietScoreDao.findByDietId(dietScore.getDietId());
        if (existing == null) {
            dietScoreDao.insert(dietScore);
            return dietScore;
        } else {
            dietScore.setId(existing.getId());
            dietScoreDao.update(dietScore);
            return dietScore;
        }
    }

    private void recalcAndUpsertDietScore(Long userId, Long dietId) {
        Diet diet = dietDao.findById(dietId);
        if (diet == null) return;

        UserBodyInfo bodyInfo = null;
        try {
            bodyInfo = userService.findByUserId(userId);
        } catch (Exception ignored) {
            // 신체정보가 아직 없는 유저면 null로 처리
        }

        var result = com.ssafy.fitmeet.domain.diet.service.score.DietScoreCalculator.calculate(diet, bodyInfo);

        DietScore score = DietScore.builder()
                .dietId(dietId)
                .score(result.score())
                .grade(result.grade())
                .feedback(result.feedback())
                .build();

        saveOrUpdateDietScore(score);
    }


    @Override
    public DietScore getDietScore(Long dietId) {
        return dietScoreDao.findByDietId(dietId);
    }

    @Override
    public DietNutritionPreviewResponse previewNutrition(DietNutritionPreviewRequest request) {
        if (request == null || request.foods() == null || request.foods().isEmpty()) {
            throw new CustomException(ErrorCode.DIET_PREVIEW_FOODS_REQUIRED);
        }

        List<DietFoodPreviewResponse> foods = request.foods().stream()
                .map(this::previewOneFood)
                .toList();

        DietNutritionTotals totals = sumTotals(foods);

        return new DietNutritionPreviewResponse(foods, totals);
    }

    private DietFoodPreviewResponse previewOneFood(DietFoodRequest item) {
        if (item == null) {
            throw new CustomException(ErrorCode.DIET_PREVIEW_FOODS_REQUIRED);
        }

        String sourceType = (item.sourceType() == null || item.sourceType().isBlank())
                ? "MANUAL"
                : item.sourceType().toUpperCase();

        if ("MANUAL".equals(sourceType)) {
            return new DietFoodPreviewResponse(
                    null,
                    item.foodNmKr(),
                    item.foodCode(),
                    "MANUAL",
                    item.intakeGram(),
                    zero(item.kcal()),
                    zero(item.carbohydrate()),
                    zero(item.protein()),
                    zero(item.fat()),
                    zero(item.sugar()),
                    zero(item.sodium()),
                    zero(item.dietaryFiber())
            );
        }

        if (item.intakeGram() == null || item.intakeGram().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.DIET_INTAKE_GRAM_REQUIRED);
        }

        if (item.foodCode() == null || item.foodCode().isBlank()) {
            throw new CustomException(ErrorCode.DIET_FOOD_CODE_REQUIRED);
        }

        Meal meal = mealService.getOrFetchMealByFoodCd(item.foodCode());
        MealNutrition nutrition = mealService.getOrFetchNutritionByFoodCd(item.foodCode());

        BigDecimal baseAmount = nutrition.getBaseAmountG();
        BigDecimal intakeGram = item.intakeGram();

        BigDecimal kcal = mealService.calculateScaled(baseAmount, nutrition.getKcal(), intakeGram);
        BigDecimal carbohydrate = mealService.calculateScaled(baseAmount, nutrition.getCarbohydrate(), intakeGram);
        BigDecimal protein = mealService.calculateScaled(baseAmount, nutrition.getProtein(), intakeGram);
        BigDecimal fat = mealService.calculateScaled(baseAmount, nutrition.getFat(), intakeGram);
        BigDecimal sugar = mealService.calculateScaled(baseAmount, nutrition.getSugar(), intakeGram);
        BigDecimal sodium = mealService.calculateScaled(baseAmount, nutrition.getSodium(), intakeGram);
        BigDecimal dietaryFiber = mealService.calculateScaled(baseAmount, nutrition.getDietaryFiber(), intakeGram);

        return new DietFoodPreviewResponse(
                meal.getId(),
                meal.getFoodNmKr(),
                meal.getFoodCd(),
                sourceType, // API or IMAGE
                intakeGram,
                zero(kcal),
                zero(carbohydrate),
                zero(protein),
                zero(fat),
                zero(sugar),
                zero(sodium),
                zero(dietaryFiber)
        );
    }

    private DietNutritionTotals sumTotals(List<DietFoodPreviewResponse> foods) {
        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalCarbo = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalSugar = BigDecimal.ZERO;
        BigDecimal totalSodium = BigDecimal.ZERO;
        BigDecimal totalDietaryFiber = BigDecimal.ZERO;

        for (DietFoodPreviewResponse f : foods) {
            totalKcal = totalKcal.add(zero(f.kcal()));
            totalCarbo = totalCarbo.add(zero(f.carbohydrate()));
            totalProtein = totalProtein.add(zero(f.protein()));
            totalFat = totalFat.add(zero(f.fat()));
            totalSugar = totalSugar.add(zero(f.sugar()));
            totalSodium = totalSodium.add(zero(f.sodium()));
            totalDietaryFiber = totalDietaryFiber.add(zero(f.dietaryFiber()));
        }

        return new DietNutritionTotals(
                totalKcal,
                totalCarbo,
                totalProtein,
                totalFat,
                totalSugar,
                totalSodium,
                totalDietaryFiber
        );
    }
}
