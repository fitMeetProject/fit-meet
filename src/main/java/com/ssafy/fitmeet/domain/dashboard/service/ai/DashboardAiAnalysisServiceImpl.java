package com.ssafy.fitmeet.domain.dashboard.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.fitmeet.domain.dashboard.dao.DietAiWeeklyAnalysisLogDao;
import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.*;
import com.ssafy.fitmeet.domain.dashboard.entity.DietAiWeeklyAnalysisLog;
import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import com.ssafy.fitmeet.domain.dashboard.service.UserDailyScoreService;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.diet.service.UserDailyNutritionService;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.service.UserService;
import com.ssafy.fitmeet.global.config.ai.OpenAiChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class DashboardAiAnalysisServiceImpl implements DashboardAiAnalysisService {

    private final UserService userService;
    private final UserDailyNutritionService userDailyNutritionService;
    private final UserDailyScoreService userDailyScoreService;

    private final OpenAiChatClient openAiChatClient;
    private final WeeklyAiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    private final DietAiWeeklyAnalysisLogDao logDao;

    @Override
    @Transactional
    public WeeklyAiAnalysisResponse analyzeWeekly(Long userId, LocalDate endDate) {
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        LocalDate start = end.minusDays(6);

        // 점수 리프레시
        List<UserDailyScore> scores = userDailyScoreService.refreshWeek(userId, end);
        int avgDailyScore = (int) Math.round(scores.stream().mapToInt(UserDailyScore::getScore).average().orElse(0));

        // 영양소 조회
        List<UserDailyNutrition> nuts = userDailyNutritionService.getRange(userId, start, end);

        // 기록일수
        int dietLoggedDays = (int) nuts.stream().filter(n -> n.getTotalKcal() != null && n.getTotalKcal().doubleValue() > 0).count();

        // 7일 평균
        double avgKcal = avg(nuts, v(UserDailyNutrition::getTotalKcal));
        double avgCarbo = avg(nuts, v(UserDailyNutrition::getTotalCarbohydrate));
        double avgProtein = avg(nuts, v(UserDailyNutrition::getTotalProtein));
        double avgFat = avg(nuts, v(UserDailyNutrition::getTotalFat));
        double avgSugar = avg(nuts, v(UserDailyNutrition::getTotalSugar));
        double avgSodium = avg(nuts, v(UserDailyNutrition::getTotalSodium));

        // 신체정보
        UserBodyInfo bodyInfo = userService.findByUserId(userId);

        Integer bmr = (bodyInfo != null) ? bodyInfo.getBmr() : null;
        Integer dailyKcal = (bodyInfo != null) ? bodyInfo.getDailyKcal() : null;
        String gender = (bodyInfo != null) ? bodyInfo.getGender() : null;
        Double heightCm = (bodyInfo != null) ? bodyInfo.getHeightCm() : null;
        Double weightKg = (bodyInfo != null) ? bodyInfo.getWeightKg() : null;

        // 4) 운동
        int workoutDays = 0;
        int workoutTotalMinutes = 0;
        List<String> workoutTypes = List.of();

        WeeklyAiInput input = new WeeklyAiInput(
                start, end,
                gender, heightCm, weightKg, bmr, dailyKcal,
                avgDailyScore,
                avgKcal, avgCarbo, avgProtein, avgFat, avgSugar, avgSodium,
                dietLoggedDays,
                workoutDays, workoutTotalMinutes, workoutTypes
        );

        String systemPrompt = promptBuilder.systemPromptKo();
        String userPrompt = promptBuilder.userPromptKo(input);

        String rawJson = null;
        WeeklyAiResult analysis;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            rawJson = openAiChatClient.chat(systemPrompt, userPrompt);

            if (rawJson == null || rawJson.trim().equals("{}")) {
                throw new IllegalStateException("Empty JSON returned by model");
            }

            // GPT JSON -> WeeklyAiResult 매핑
            analysis = objectMapper.readValue(rawJson, WeeklyAiResult.class);

        } catch (Exception e) {
            status = "FALLBACK";
            errorMessage = e.getMessage();
            log.warn("AI weekly analysis failed. fallback applied. userId={}, endDate={}, err={}", userId, end, errorMessage);

            analysis = fallbackWeeklyResult(avgDailyScore, dietLoggedDays, workoutDays);
            rawJson = safeWriteJson(analysis);
        }

        // 로그 저장
        DietAiWeeklyAnalysisLog logEntity = DietAiWeeklyAnalysisLog.builder()
                .userId(userId)
                .startDate(start)
                .endDate(end)
                .requestSystemPrompt(systemPrompt)
                .requestUserPrompt(userPrompt)
                .responseJson(rawJson)
                .weeklyScore(analysis.weeklyScore())
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        logDao.insert(logEntity);

        return new WeeklyAiAnalysisResponse(start, end, avgDailyScore, analysis, status);
    }

    private WeeklyAiResult fallbackWeeklyResult(int avgDailyScore, int dietLoggedDays, int workoutDays) {
        int score = 50;
        if (dietLoggedDays >= 5) score += 10;
        if (workoutDays >= 3) score += 10;
        if (avgDailyScore >= 70) score += 10;

        return new WeeklyAiResult(
                Math.max(0, Math.min(100, score)),
                "AI 응답 형식 오류 또는 네트워크 문제로 기본 규칙 기반 분석이 적용되었습니다.",
                List.of("최근 7일 데이터(점수/식단 기록)를 기반으로 최소 진단을 수행했습니다."),
                List.of("AI 상세 분석이 제한되어 구체적인 맞춤 추천은 축약되었습니다."),
                new DietRecommendations(
                        List.of("단백질/채소 비중을 확보하고, 당류/나트륨을 점검하세요."),
                        List.of("닭가슴살/계란/두부", "현미/고구마", "브로콜리/샐러드"),
                        List.of("가공식품", "단 음료", "야식")
                ),
                new ExerciseRecommendations(
                        "주 3~4회, 30분 이상 중강도 유산소를 권장합니다.",
                        "주 2~3회 전신 근력(하체/등/코어) 중심으로 진행하세요.",
                        "유산소-근력 번갈아 진행하고 최소 1일은 휴식일로 설정하세요."
                )
        );
    }

    private String safeWriteJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private interface Extractor { double get(UserDailyNutrition n); }

    private Extractor v(java.util.function.Function<UserDailyNutrition, Number> f) {
        return n -> {
            Number num = f.apply(n);
            return (num == null) ? 0.0 : num.doubleValue();
        };
    }

    private double avg(List<UserDailyNutrition> list, Extractor ex) {
        if (list == null || list.isEmpty()) return 0.0;
        return list.stream().mapToDouble(ex::get).average().orElse(0.0);
    }
}
