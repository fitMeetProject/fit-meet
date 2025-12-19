package com.ssafy.fitmeet.domain.diet.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.fitmeet.global.config.ai.OpenAiClient;
import com.ssafy.fitmeet.global.config.ai.OpenAiDto;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class DietImageAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final DietImagePromptBuilder promptBuilder;

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
}

