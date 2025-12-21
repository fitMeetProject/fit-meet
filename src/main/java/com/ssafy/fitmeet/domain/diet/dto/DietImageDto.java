package com.ssafy.fitmeet.domain.diet.dto;

import java.math.BigDecimal;
import java.util.List;

public class DietImageDto {

    /**
     * /api/diet/images/analyze 응답
     */
    public record FoodAnalyzeMappedResponse(
            List<MappedFoodItem> matchedItems,
            List<UnmatchedFoodItem> unmatchedItems,
            boolean needsUserPortion,
            String notes
    ) { }

    /**
     * 선택 가능한 AI 후보(이미 DB/공공데이터 매핑 수행)
     */
    public record MappedFoodItem(
            String uiName,          // 화면에 보여줄 대표명(보통 AI nameKo)
            String aiNameKo,
            String aiNameEn,
            double confidence,
            MatchStatus matchStatus, // EXACT / AMBIGUOUS
            List<FoodCandidate> candidates
    ) { }

    /**
     * 실제로 선택될 수 있는 후보(= foodCode 존재)
     */
    public record FoodCandidate(
            String foodCode,
            String foodNmKr,
            String category,
            BigDecimal baseAmountG // 있으면 좋음(없어도 됨)
    ) { }

    public enum MatchStatus {
        EXACT, // 후보 1개 → 클릭 즉시 foodCode 확정 가능
        AMBIGUOUS // 후보 여러개 → 클릭 시 후보 선택 UI 필요
    }

    /**
     * 매핑 실패 항목(선택 불가)
     */
    public record UnmatchedFoodItem(
            String uiName,
            String aiNameKo,
            String aiNameEn,
            double confidence,
            String reason
    ) { }
}
