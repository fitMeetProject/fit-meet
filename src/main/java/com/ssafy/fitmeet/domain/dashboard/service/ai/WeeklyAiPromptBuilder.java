package com.ssafy.fitmeet.domain.dashboard.service.ai;

import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.WeeklyAiInput;
import org.springframework.stereotype.Component;

@Component
public class WeeklyAiPromptBuilder {

    public String systemPromptKo() {
        return """
        You are a professional nutritionist and fitness coach AI.
        
        All response VALUES must be written in Korean.
        JSON keys must remain in English exactly as specified.
        Your response MUST be valid JSON only.
        Do NOT include explanations outside JSON.
        Do NOT use markdown.
        Do NOT add any additional fields.
        If you cannot follow the format, return {}.
        """;
    }

    public String userPromptKo(WeeklyAiInput in) {
        return """
        `다음은 사용자의 최근 7일간 건강 데이터입니다.
        이 데이터를 종합 분석하여 현재 상태를 진단하고,
        식단 및 운동 개선 방향을 제안하세요.
        
        [분석 기간]
        %s ~ %s
        
        [신체 정보]
        - 성별: %s
        - 키(cm): %s
        - 몸무게(kg): %s
        - BMR: %s
        - 목표 섭취 칼로리(일): %s
        
        [대시보드 점수(일별 점수 평균)]
        - 평균 점수: %d점
        
        [식단 요약(7일 평균)]
        - 평균 섭취 칼로리: %.0f kcal
        - 평균 탄수화물: %.1f g
        - 평균 단백질: %.1f g
        - 평균 지방: %.1f g
        - 평균 당류: %.1f g
        - 평균 나트륨: %.1f mg
        - 식단 기록 일수: %d일
        
        [운동 요약]
        - 운동 일수: %d일
        - 총 운동 시간: %d분
        - 운동 종류: %s
        
        아래 JSON 형식으로만 결과를 반환하세요. (모든 값은 한국어)
        {
          "weeklyScore": 0,
          "summary": "",
          "strengths": [],
          "weaknesses": [],
          "dietRecommendations": {
            "focusPoints": [],
            "recommendedFoods": [],
            "foodsToReduce": []
          },
          "exerciseRecommendations": {
            "cardio": "",
            "strength": "",
            "weeklyPlan": ""
          }
        }
        """.formatted(
                in.startDate(), in.endDate(),
                nvl(in.gender()), nvl(in.heightCm()), nvl(in.weightKg()),
                nvl(in.bmr()), nvl(in.dailyKcal()),
                in.avgDailyScore(),
                in.avgKcal(), in.avgCarbo(), in.avgProtein(), in.avgFat(), in.avgSugar(), in.avgSodium(),
                in.dietLoggedDays(),
                in.workoutDays(), in.workoutTotalMinutes(), String.valueOf(in.workoutTypes())
        );
    }

    private String nvl(Object v) {
        return (v == null) ? "정보없음" : String.valueOf(v);
    }
}
