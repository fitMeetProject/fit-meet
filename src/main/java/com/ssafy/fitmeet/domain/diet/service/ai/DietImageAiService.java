package com.ssafy.fitmeet.domain.diet.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.fitmeet.domain.diet.dto.DietImageDto;
import com.ssafy.fitmeet.global.config.ai.OpenAiClient;
import com.ssafy.fitmeet.global.config.ai.OpenAiDto;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class DietImageAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final DietImagePromptBuilder promptBuilder;

    // 신규: 매핑 컴포넌트 주입
    private final DietImageFoodMapper foodMapper;

    /**
     * AI 결과만 파싱해서 반환(유지)
     */
    public OpenAiDto.FoodAnalyzeResponse analyze(String imageUrl) {

        String prompt = promptBuilder.promptForFoodDetection();

        String jsonText;
        try {
            jsonText = openAiClient.analyzeFoodImageJsonText(imageUrl, prompt);
            log.info("[VISION RAW OUTPUT] {}", jsonText);
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_UPSTREAM_ERROR, e.getMessage());
        }

        if (jsonText == null || jsonText.isBlank()) {
            throw new CustomException(ErrorCode.AI_INVALID_RESPONSE, "empty output_text");
        }

        try {
            OpenAiDto.FoodAnalyzeResponse res = objectMapper.readValue(jsonText, OpenAiDto.FoodAnalyzeResponse.class);

            if (res.items() == null || res.items().isEmpty()) {
                throw new CustomException(ErrorCode.AI_INVALID_RESPONSE, jsonText);
            }
            return res;

        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_INVALID_RESPONSE, jsonText);
        }
    }

    /**
     * AI 분석 + DB/공공데이터 매핑까지 수행해서 선택 가능한 후보만 반환
     */
    public DietImageDto.FoodAnalyzeMappedResponse analyzeAndMap(String imageUrl) {

        OpenAiDto.FoodAnalyzeResponse ai = analyze(imageUrl);

        List<DietImageDto.MappedFoodItem> matchedItems = new ArrayList<>();
        List<DietImageDto.UnmatchedFoodItem> unmatchedItems = new ArrayList<>();

        for (OpenAiDto.FoodCandidate item : ai.items()) {
            String nameKo = item.nameKo();
            String nameEn = item.nameEn();

            DietImageFoodMapper.MappingResult mr = foodMapper.mapToCandidates(nameKo, nameEn, item.confidence());

            if (!mr.matched() || mr.candidates() == null || mr.candidates().isEmpty()) {
                unmatchedItems.add(new DietImageDto.UnmatchedFoodItem(
                        nameKo != null ? nameKo : (nameEn != null ? nameEn : "unknown"),
                        nameKo,
                        nameEn,
                        item.confidence(),
                        mr.reason() != null ? mr.reason() : "NO_MATCH"
                ));
                continue;
            }

            matchedItems.add(new DietImageDto.MappedFoodItem(
                    nameKo != null ? nameKo : mr.candidates().get(0).foodNmKr(),
                    nameKo,
                    nameEn,
                    item.confidence(),
                    mr.matchStatus(),
                    mr.candidates()
            ));
        }

        return new DietImageDto.FoodAnalyzeMappedResponse(
                matchedItems,
                unmatchedItems,
                ai.needsUserPortion(),
                ai.notes()
        );
    }
}
