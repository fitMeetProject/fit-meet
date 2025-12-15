package com.ssafy.fitmeet.domain.diet.dto;

import com.ssafy.fitmeet.domain.diet.entity.Diet;
import com.ssafy.fitmeet.domain.diet.entity.DietInfo;
import com.ssafy.fitmeet.domain.diet.entity.DietScore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class DietDto {

    /**
     * 식단 생성 요청 DTO
     *
     * sourceType: 한 끼 전체 기준
     *  - "MANUAL" : 사용자가 음식 + 영양성분 직접 입력
     *  - "API"    : 공공데이터 기반
     *  - "IMAGE"  : 이미지 분석 기반
     */
    public record DietCreateRequest(
            LocalDateTime date,
            String mealType,
            String description,
            String imageUrl,
            String sourceType, // "MANUAL" / "API" / "IMAGE"
            Boolean isPublic,
            List<DietFoodRequest> foods // 한 끼 안의 음식 목록
    ) {}

    /**
     * 식단 수정 요청 DTO
     */
    public record DietUpdateRequest(
            String mealType,
            String description,
            String imageUrl,
            String sourceType, // "MANUAL" / "API" / "IMAGE"
            Boolean isPublic,
            List<DietFoodRequest> foods
    ) {}

    /**
     * 음식 입력 DTO
     *
     * - sourceType = "MANUAL"
     *   → foodNmKr, intakeGram, kcal/carbohydrate/protein/... 직접 채워서 옴
     *
     * - sourceType = "API" / "IMAGE"
     *   → foodCode + intakeGram 만 채워서 옴 (영양소는 null이어도 됨)
     */
    public record DietFoodRequest(
            String foodNmKr,
            String foodCode,
            String sourceType,
            BigDecimal intakeGram,
            BigDecimal kcal,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal sugar,
            BigDecimal sodium,
            BigDecimal dietaryFiber
    ) { }

    /**
     * 식단 점수 저장/수정 요청 DTO
     */
    public record DietScoreRequest(
            Integer score,
            String grade,
            String feedback
    ) { }

    /**
     * 캘린더/리스트 조회용 간단 요약 응답 DTO
     */
    public record DietSummaryResponse(
            Long id,
            LocalDateTime date,
            String mealType,
            String description,
            String imageUrl,
            BigDecimal totalKcal,
            BigDecimal totalCarbohydrate,
            BigDecimal totalProtein,
            BigDecimal totalFat,
            Boolean isPublic
    ) {
        public static DietSummaryResponse from(Diet diet) {
            return new DietSummaryResponse(
                    diet.getId(),
                    diet.getDate(),
                    diet.getMealType(),
                    diet.getDescription(),
                    diet.getImageUrl(),
                    diet.getTotalKcal(),
                    diet.getTotalCarbohydrate(),
                    diet.getTotalProtein(),
                    diet.getTotalFat(),
                    diet.getIsPublic()
            );
        }
    }

    /**
     * 한 끼 상세 조회용 음식 정보 응답 DTO
     */
    public record DietFoodResponse(
            Long id,
            Long mealId,
            String foodNmKr,
            String foodCode,
            String sourceType,
            BigDecimal intakeGram,
            BigDecimal kcal,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal sugar,
            BigDecimal sodium,
            BigDecimal dietaryFiber
    ) {
        public static DietFoodResponse from(DietInfo info) {
            return new DietFoodResponse(
                    info.getId(),
                    info.getMealId(),
                    info.getFoodNmKr(),
                    info.getFoodCode(),
                    info.getSourceType(),
                    info.getIntakeGram(),
                    info.getKcal(),
                    info.getCarbohydrate(),
                    info.getProtein(),
                    info.getFat(),
                    info.getSugar(),
                    info.getSodium(),
                    info.getDietaryFiber()
            );
        }
    }

    /**
     * 식단 점수 응답 DTO
     */
    public record DietScoreResponse(
            Long id,
            Long dietId,
            Integer score,
            String grade,
            String feedback
    ) {
        public static DietScoreResponse from(DietScore score) {
            if (score == null) return null;
            return new DietScoreResponse(
                    score.getId(),
                    score.getDietId(),
                    score.getScore(),
                    score.getGrade(),
                    score.getFeedback()
            );
        }
    }

    /**
     * 식단 상세 조회 응답 (Diet + Foods + Score)
     */
    public record DietDetailResponse(
            Long id,
            LocalDateTime date,
            String mealType,
            String description,
            String imageUrl,
            String sourceType,
            Boolean isPublic,
            BigDecimal totalKcal,
            BigDecimal totalCarbohydrate,
            BigDecimal totalProtein,
            BigDecimal totalFat,
            BigDecimal totalSugar,
            BigDecimal totalSodium,
            List<DietFoodResponse> foods,
            DietScoreResponse score
    ) {
        public static DietDetailResponse of(Diet diet, List<DietInfo> infos, DietScore score) {
            List<DietFoodResponse> foodResponses = (infos == null)
                    ? List.of()
                    : infos.stream()
                    .map(DietFoodResponse::from)
                    .collect(Collectors.toList());

            return new DietDetailResponse(
                    diet.getId(),
                    diet.getDate(),
                    diet.getMealType(),
                    diet.getDescription(),
                    diet.getImageUrl(),
                    diet.getSourceType(),
                    diet.getIsPublic(),
                    diet.getTotalKcal(),
                    diet.getTotalCarbohydrate(),
                    diet.getTotalProtein(),
                    diet.getTotalFat(),
                    diet.getTotalSugar(),
                    diet.getTotalSodium(),
                    foodResponses,
                    DietScoreResponse.from(score)
            );
        }
    }

    /**
     * 저장 전 영양소 미리보기 요청
     * - foods: DietCreate/Update 때 쓰는 foods 구조 그대로 재사용
     */
    public record DietNutritionPreviewRequest(
            List<DietFoodRequest> foods
    ) {}

    /**
     * 저장 전 영양소 미리보기 응답
     * - foods: 개별 음식 결과
     * - totals: 한 끼 합계 (UI 상단 카드용)
     */
    public record DietNutritionPreviewResponse(
            List<DietFoodPreviewResponse> foods,
            DietNutritionTotals totals
    ) {}

    public record DietNutritionTotals(
            BigDecimal totalKcal,
            BigDecimal totalCarbohydrate,
            BigDecimal totalProtein,
            BigDecimal totalFat,
            BigDecimal totalSugar,
            BigDecimal totalSodium,
            BigDecimal totalDietaryFiber
    ) {}

    /**
     * 저장 전 개별 음식 결과
     * - API/IMAGE: 서버가 계산한 영양소(섭취량 반영) 반환
     * - MANUAL: 입력값 그대로 반환
     */
    public record DietFoodPreviewResponse(
            Long mealId,
            String foodNmKr,
            String foodCode,
            String sourceType,
            BigDecimal intakeGram,
            BigDecimal kcal,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal sugar,
            BigDecimal sodium,
            BigDecimal dietaryFiber
    ) { }


}
