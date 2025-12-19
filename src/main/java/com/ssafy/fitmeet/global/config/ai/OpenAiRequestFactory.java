package com.ssafy.fitmeet.global.config.ai;

import java.util.List;
import java.util.Map;

public class OpenAiRequestFactory {

    private OpenAiRequestFactory() {}

    /**
     * 텍스트 전용
     */
    public static Map<String, Object> chatRequest(String model, String systemPrompt, String userPrompt) {
        return Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "input_text",
                                                "text", systemPrompt + "\n" + userPrompt
                                        )
                                )
                        )
                )
        );
    }

    /**
     * 이미지 + 텍스트 (Vision)
     */
    public static Map<String, Object> visionRequest(
            String model,
            String prompt,
            String imageUrl
    ) {
        return Map.of(
                "model", model,
                "input", List.of(
                        Map.of(
                                "role", "user",
                                "content", List.of(
                                        Map.of(
                                                "type", "input_text",
                                                "text", prompt
                                        ),
                                        Map.of(
                                                "type", "input_image",
                                                "image_url", imageUrl
                                        )
                                )
                        )
                ),

                "text", Map.of(
                        "format", Map.of(
                                "type", "json_object"
                        )
                )
        );
    }


}
