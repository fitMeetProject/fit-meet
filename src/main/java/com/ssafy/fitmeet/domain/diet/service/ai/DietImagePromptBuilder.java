package com.ssafy.fitmeet.domain.diet.service.ai;

import org.springframework.stereotype.Component;

@Component
public class DietImagePromptBuilder {

    public String promptForFoodDetection() {
        return """
        너는 식단 기록을 돕는 영양 분석 AI야.

        주어진 음식 사진을 보고,
        사용자가 기록할 수 있도록 "음식 후보"를 분석해줘.

        규칙:
        1. 음식 후보는 최대 3개
        2. 반드시 JSON ONLY로 응답
        3. 각 항목은 다음 필드를 포함:
           - nameKo (한글 음식명)
           - nameEn (영문 음식명)
           - confidence (0~1)
        4. 신뢰도가 낮거나 여러 음식이 섞여 있으면
           needsUserPortion = true
        5. 설명은 notes에 간단히 작성

        응답 JSON 형식:
        {
          "items": [
            { "nameKo": "...", "nameEn": "...", "confidence": 0.87 }
          ],
          "needsUserPortion": false,
          "notes": "..."
        }
        
        반드시 위 JSON 형식 그대로만 출력하고, 코드블록(```), 설명 문장, 여분 텍스트를 절대 포함하지 마.
        """;
    }
}
