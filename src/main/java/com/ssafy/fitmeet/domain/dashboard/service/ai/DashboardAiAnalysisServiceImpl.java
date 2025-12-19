package com.ssafy.fitmeet.domain.dashboard.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.fitmeet.domain.dashboard.dao.DietAiWeeklyAnalysisLogDao;
import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto;
import com.ssafy.fitmeet.domain.dashboard.entity.DietAiWeeklyAnalysisLog;
import com.ssafy.fitmeet.domain.dashboard.entity.UserDailyScore;
import com.ssafy.fitmeet.domain.dashboard.service.UserDailyScoreService;
import com.ssafy.fitmeet.domain.diet.entity.UserDailyNutrition;
import com.ssafy.fitmeet.domain.diet.service.UserDailyNutritionService;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.domain.user.service.UserService;
import com.ssafy.fitmeet.global.config.ai.OpenAiClient;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class DashboardAiAnalysisServiceImpl implements DashboardAiAnalysisService {

    private final UserService userService;
    private final UserDailyNutritionService userDailyNutritionService;
    private final UserDailyScoreService userDailyScoreService;

    private final OpenAiClient openAiClient;
    private final WeeklyAiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    private final DietAiWeeklyAnalysisLogDao logDao;

    @Override
    @Transactional
    public DashboardDto.WeeklyAiAnalysisResponse analyzeWeekly(Long userId, LocalDate endDate) {

        LocalDate end = (endDate != null) ? endDate : LocalDate.now();
        LocalDate start = end.minusDays(6);

        // 주간 점수 refresh + 평균
        List<UserDailyScore> scores = userDailyScoreService.refreshWeek(userId, end);
        int avgDailyScore = (int) Math.round(scores.stream().mapToInt(UserDailyScore::getScore).average().orElse(0));

        // 주간 영양 range + 평균
        List<UserDailyNutrition> nuts = userDailyNutritionService.getRange(userId, start, end);

        int dietLoggedDays = (int) nuts.stream().filter(n -> n.getTotalKcal() != null && n.getTotalKcal().doubleValue() > 0).count();

        double avgKcal = avg(nuts, UserDailyNutrition::getTotalKcal);
        double avgCarbo = avg(nuts, UserDailyNutrition::getTotalCarbohydrate);
        double avgProtein = avg(nuts, UserDailyNutrition::getTotalProtein);
        double avgFat = avg(nuts, UserDailyNutrition::getTotalFat);
        double avgSugar = avg(nuts, UserDailyNutrition::getTotalSugar);
        double avgSodium = avg(nuts, UserDailyNutrition::getTotalSodium);

        // 신체정보
        UserBodyInfo bodyInfo = userService.findByUserId(userId);

        Integer bmr = (bodyInfo != null) ? bodyInfo.getBmr() : null;
        Integer dailyKcal = (bodyInfo != null) ? bodyInfo.getDailyKcal() : null;
        String gender = (bodyInfo != null) ? bodyInfo.getGender() : null;
        Double heightCm = (bodyInfo != null) ? bodyInfo.getHeightCm() : null;
        Double weightKg = (bodyInfo != null) ? bodyInfo.getWeightKg() : null;

        // 운동
        int workoutDays = 0;
        int workoutTotalMinutes = 0;
        List<String> workoutTypes = List.of();

        DashboardDto.WeeklyAiInput input = new DashboardDto.WeeklyAiInput(
                start, end, gender, heightCm, weightKg, bmr, dailyKcal, avgDailyScore,
                avgKcal, avgCarbo, avgProtein, avgFat, avgSugar, avgSodium, dietLoggedDays,
                workoutDays, workoutTotalMinutes, workoutTypes
        );

        // Prompt 생성
        String systemPrompt = promptBuilder.systemPromptKo();
        String userPrompt = promptBuilder.userPromptKo(input);

        // OpenAI 호출 + 파싱 + 정책
        String rawJson = null;
        DashboardDto.WeeklyAiResult analysis;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            rawJson = openAiClient.chat(systemPrompt, userPrompt);

            if (rawJson == null || rawJson.trim().equals("{}")) {
                throw new CustomException(ErrorCode.AI_INVALID_RESPONSE, "빈 JSON 응답({})이 반환되었습니다.");
            }

            analysis = objectMapper.readValue(rawJson, DashboardDto.WeeklyAiResult.class);

        } catch (CustomException ce) {

            if (ce.getErrorCode() == ErrorCode.AI_RATE_LIMITED
                    || ce.getErrorCode() == ErrorCode.AI_UPSTREAM_AUTH_FAILED
                    || ce.getErrorCode() == ErrorCode.AI_INVALID_REQUEST
                    || ce.getErrorCode() == ErrorCode.AI_INVALID_RESPONSE) {

                status = "FAIL";
                errorMessage = ce.getMessage();
                saveLog(userId, start, end, systemPrompt, userPrompt, rawJson, null, status, errorMessage);
                throw ce; // GlobalExceptionHandler가 처리
            }

            status = "FALLBACK";
            errorMessage = ce.getMessage();
            analysis = fallbackWeeklyResult(avgDailyScore, dietLoggedDays, workoutDays);
            rawJson = safeWriteJson(analysis);
            saveLog(userId, start, end, systemPrompt, userPrompt, rawJson, analysis.weeklyScore(), status, errorMessage);

            return buildWeeklyAiAnalysisResponse(start, end, avgDailyScore, analysis, status);

        } catch (Exception e) {
            status = "FAIL";
            errorMessage = e.getMessage();
            saveLog(userId, start, end, systemPrompt, userPrompt, rawJson, null, status, errorMessage);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        // SUCCESS
        saveLog(userId, start, end, systemPrompt, userPrompt, rawJson, analysis.weeklyScore(), status, null);
        return buildWeeklyAiAnalysisResponse(start, end, avgDailyScore, analysis, status);
    }

    private DashboardDto.WeeklyAiAnalysisResponse buildWeeklyAiAnalysisResponse(LocalDate start, LocalDate end, int avgDailyScore, DashboardDto.WeeklyAiResult analysis, String status) {
        return new DashboardDto.WeeklyAiAnalysisResponse(start, end, avgDailyScore, analysis, status);
    }

    private DashboardDto.WeeklyAiResult fallbackWeeklyResult(int avgDailyScore, int dietLoggedDays, int workoutDays) {
        int score = 50;
        if (dietLoggedDays >= 5) score += 10;
        if (workoutDays >= 3) score += 10;
        if (avgDailyScore >= 70) score += 10;

        return new DashboardDto.WeeklyAiResult(
                Math.max(0, Math.min(100, score)),
                "AI 응답 문제로 기본 규칙 기반 분석이 적용되었습니다.",
                List.of("최근 7일 데이터(점수/식단 기록)를 기반으로 기본 진단을 수행했습니다."),
                List.of("AI 상세 분석이 제한되어 맞춤 추천이 축약되었습니다."),
                new DashboardDto.DietRecommendations(
                        List.of("단백질/채소 비중을 확보하고, 당류/나트륨을 점검하세요."),
                        List.of("닭가슴살/계란/두부", "현미/고구마", "브로콜리/샐러드"),
                        List.of("가공식품", "단 음료", "야식")
                ),
                new DashboardDto.ExerciseRecommendations(
                        "주 3~4회, 30분 이상 중강도 유산소를 권장합니다.",
                        "주 2~3회 전신 근력(하체/등/코어) 중심으로 진행하세요.",
                        "유산소-근력 번갈아 진행하고 최소 1일은 휴식일로 설정하세요."
                )
        );
    }

    private void saveLog(Long userId, LocalDate start, LocalDate end,
                         String systemPrompt, String userPrompt,
                         String responseJson, Integer weeklyScore,
                         String status, String errorMessage) {

        DietAiWeeklyAnalysisLog logEntity = DietAiWeeklyAnalysisLog.builder()
                .userId(userId)
                .startDate(start)
                .endDate(end)
                .requestSystemPrompt(systemPrompt)
                .requestUserPrompt(userPrompt)
                .responseJson(responseJson)
                .weeklyScore(weeklyScore)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        logDao.insert(logEntity);
    }

    private String safeWriteJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private double avg(List<UserDailyNutrition> list, java.util.function.Function<UserDailyNutrition, Number> f) {
        if (list == null || list.isEmpty()) return 0.0;
        return list.stream()
                .map(f)
                .filter(Objects::nonNull)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }
}

