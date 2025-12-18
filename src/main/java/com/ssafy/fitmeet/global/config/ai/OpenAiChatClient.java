package com.ssafy.fitmeet.global.config.ai;

import com.ssafy.fitmeet.global.config.ai.OpenAiChatDto.OpenAiChatRequest;
import com.ssafy.fitmeet.global.config.ai.OpenAiChatDto.OpenAiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiChatClient {

    private final WebClient openAiWebClient;

    @Value("${open-api.gpt.model}")
    private String model;

    public String chat(String systemPrompt, String userPrompt) {
        OpenAiChatRequest req = new OpenAiChatRequest(
                model,
                List.of(
                        new OpenAiChatRequest.Message("system", systemPrompt),
                        new OpenAiChatRequest.Message("user", userPrompt)
                )
        );

        OpenAiChatResponse res = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(OpenAiChatResponse.class)
                .block();

        return res.getChoices().get(0).getMessage().getContent();
    }
}
