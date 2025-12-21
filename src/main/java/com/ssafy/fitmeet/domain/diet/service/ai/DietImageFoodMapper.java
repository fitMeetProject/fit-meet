package com.ssafy.fitmeet.domain.diet.service.ai;

import com.ssafy.fitmeet.domain.diet.dto.DietImageDto;
import com.ssafy.fitmeet.domain.meal.entity.Meal;
import com.ssafy.fitmeet.domain.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DietImageFoodMapper {

    private final MealService mealService;

    // confidence 너무 낮으면 매핑 시도 자체를 줄이고 unmatched로
    private static final double MIN_CONFIDENCE_TO_MAP = 0.35;

    // 후보 최대 개수
    private static final int MAX_CANDIDATES = 5;

    // 조리어/수식어 제거(최소 MVP 버전)
    private static final List<Pattern> COOKING_SUFFIX_PATTERNS = List.of(
            Pattern.compile("\\s*볶음\\s*"),
            Pattern.compile("\\s*구이\\s*"),
            Pattern.compile("\\s*찜\\s*"),
            Pattern.compile("\\s*탕\\s*"),
            Pattern.compile("\\s*국\\s*"),
            Pattern.compile("\\s*찌개\\s*"),
            Pattern.compile("\\s*무침\\s*"),
            Pattern.compile("\\s*조림\\s*"),
            Pattern.compile("\\s*덮밥\\s*"),
            Pattern.compile("\\s*비빔밥\\s*"),
            Pattern.compile("\\s*샐러드\\s*"),
            Pattern.compile("\\s*라면\\s*"),
            Pattern.compile("\\s*볶\\s*")
    );

    public MappingResult mapToCandidates(String nameKo, String nameEn, double confidence) {

        String uiName = pickUiName(nameKo, nameEn);

        if (confidence < MIN_CONFIDENCE_TO_MAP) {
            return MappingResult.noMatch("AI 신뢰도가 낮아 자동 매칭을 생략했습니다. (검색/직접등록 권장)");
        }

        // 1차: 원문 그대로 검색
        List<Meal> meals = searchBoth(nameKo, nameEn);
        if (!meals.isEmpty()) {
            return buildResult(uiName, meals, nameKo, nameEn);
        }

        // 2차: 조리어 제거 후 검색 (공심채 볶음 -> 공심채)
        String simplifiedKo = simplifyKoreanQuery(nameKo);
        String simplifiedEn = simplifyEnglishQuery(nameEn);

        meals = searchBoth(simplifiedKo, simplifiedEn);
        if (!meals.isEmpty()) {
            return buildResult(uiName, meals, simplifiedKo, simplifiedEn);
        }

        return MappingResult.noMatch("DB/공공데이터에서 매칭 실패");
    }

    private List<Meal> searchBoth(String ko, String en) {
        List<Meal> meals = safeSearch(ko);
        if (!meals.isEmpty()) return meals;
        return safeSearch(en);
    }

    private List<Meal> safeSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        return mealService.searchMeals(normalize(keyword), null);
    }

    private MappingResult buildResult(String uiName, List<Meal> meals, String pivotKo, String pivotEn) {

        String pivot = (pivotKo != null && !pivotKo.isBlank()) ? pivotKo : pivotEn;

        // 후보 정렬(간단하지만 효과 있음)
        List<Meal> sorted = meals.stream()
                .sorted(Comparator.comparingInt(m -> rankByName(pivot, m.getFoodNmKr())))
                .limit(MAX_CANDIDATES)
                .toList();

        List<DietImageDto.FoodCandidate> candidates = sorted.stream()
                .map(m -> new DietImageDto.FoodCandidate(
                        m.getFoodCd(),
                        m.getFoodNmKr(),
                        m.getCategory(),
                        null
                ))
                .toList();

        DietImageDto.MatchStatus status = decideMatchStatus(pivot, candidates);

        return MappingResult.matched(status, candidates);
    }

    private DietImageDto.MatchStatus decideMatchStatus(String pivot, List<DietImageDto.FoodCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (candidates.size() > 1) return DietImageDto.MatchStatus.AMBIGUOUS;

        String candName = candidates.get(0).foodNmKr();
        int r = rankByName(pivot, candName);
        return (r <= 2) ? DietImageDto.MatchStatus.EXACT : DietImageDto.MatchStatus.AMBIGUOUS;
    }

    private String pickUiName(String ko, String en) {
        if (ko != null && !ko.isBlank()) return ko.trim();
        if (en != null && !en.isBlank()) return en.trim();
        return "unknown";
    }

    private String normalize(String q) {
        return q.trim().replaceAll("\\s+", " ");
    }

    /**
     * 낮을수록 우선순위 높음
     */
    private int rankByName(String pivot, String foodNmKr) {
        if (pivot == null || pivot.isBlank() || foodNmKr == null) return 100;

        String p = pivot.replace(" ", "");
        String f = foodNmKr.replace(" ", "");

        if (f.equals(p)) return 0;
        if (f.startsWith(p)) return 1;
        if (f.contains(p)) return 2;

        // pivot 단어 중 일부 포함
        String[] tokens = pivot.split("\\s+");
        for (String t : tokens) {
            if (!t.isBlank() && f.contains(t.replace(" ", ""))) return 10;
        }
        return 50;
    }

    private String simplifyKoreanQuery(String q) {
        if (q == null || q.isBlank()) return q;
        String out = q;
        for (Pattern p : COOKING_SUFFIX_PATTERNS) {
            out = p.matcher(out).replaceAll(" ");
        }
        out = normalize(out);

        if (out.replace(" ", "").length() < 2) return q;
        return out;
    }

    private String simplifyEnglishQuery(String q) {
        if (q == null || q.isBlank()) return q;

        String out = q.toLowerCase(Locale.ROOT);
        out = out.replace("stir-fried", " ")
                .replace("fried", " ")
                .replace("boiled", " ")
                .replace("grilled", " ")
                .replace("steamed", " ")
                .replace("salad", " ")
                .replace("-", " ");
        out = normalize(out);
        if (out.replace(" ", "").length() < 2) return q;
        return out;
    }

    public record MappingResult(
            boolean matched,
            DietImageDto.MatchStatus matchStatus,
            List<DietImageDto.FoodCandidate> candidates,
            String reason
    ) {
        public static MappingResult matched(DietImageDto.MatchStatus status, List<DietImageDto.FoodCandidate> candidates) {
            return new MappingResult(true, status, candidates, null);
        }
        public static MappingResult noMatch(String reason) {
            return new MappingResult(false, null, List.of(), reason);
        }
    }
}
