package com.ssafy.fitmeet.support.seed.gen;

import com.ssafy.fitmeet.domain.dashboard.dao.DietAiWeeklyAnalysisLogDao;
import com.ssafy.fitmeet.domain.dashboard.entity.DietAiWeeklyAnalysisLog;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DietAiLogSeeder {

    private final Faker faker;
    private final DietAiWeeklyAnalysisLogDao logDao;

    @Transactional
    public void seedWeeklyAiLogs(List<Long> userIds, int weeksBack, int perUserMax) {
        // 사용자당 0~perUserMax건 생성
        for (Long userId : userIds) {
            int make = faker.number().numberBetween(0, perUserMax + 1);

            for (int i = 0; i < make; i++) {
                // 종료일은 최근 weeksBack 주 내에서 랜덤 (일요일로 정규화할 수도 있는데 seed니까 단순)
                LocalDate end = LocalDate.now().minusWeeks(faker.number().numberBetween(0, weeksBack)).minusDays(faker.number().numberBetween(0, 6));
                LocalDate start = end.minusDays(6);

                DietAiWeeklyAnalysisLog row = new DietAiWeeklyAnalysisLog();
                row.setUserId(userId);
                row.setStartDate(start);
                row.setEndDate(end);

                row.setRequestSystemPrompt("[SEED] You are a nutrition coach. Summarize weekly diet.");
                row.setRequestUserPrompt("[SEED] user weekly diet summary request.");

                // JSON 컬럼이므로 문자열로 넣어도 MySQL에서는 JSON 파싱됨(형식만 JSON이면 OK)
                row.setResponseJson(sampleResponseJson());
                row.setWeeklyScore(faker.number().numberBetween(50, 96));
                row.setStatus("SUCCESS");
                row.setErrorMessage(null);

                row.setCreatedAt(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 30)));
                row.setUpdatedAt(LocalDateTime.now());

                logDao.insert(row);
            }
        }
    }

    private String sampleResponseJson() {
        // JSON wordy 하지 않게, 화면 테스트용 최소
        return """
        {
          "summary": "이번 주는 전반적으로 균형이 좋았습니다.",
          "strengths": ["단백질 섭취가 꾸준함", "과식 빈도가 낮음"],
          "improvements": ["나트륨 섭취를 약간 줄여보세요", "채소 비중을 늘려보세요"],
          "recommendations": {
            "diet": ["아침에 과일/요거트 추가", "저녁 탄수화물 10% 감량"],
            "workout": ["주 3회 20분 유산소", "하체 근력 2회"]
          }
        }
        """;
    }
}
