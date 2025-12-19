package com.ssafy.fitmeet.global.config.ai;

import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;

    @Value("${open-api.gpt.model}")
    private String model;

    public String chatRaw(Object requestBody) {
        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapToCustomException)
                .bodyToMono(String.class)
                .retryWhen(retrySpecForTransientErrors())
                .block();
    }

    public String responsesText(Map<String, Object> requestBody) {
        OpenAiDto.OpenAiResponse res = openAiWebClient.post()
                .uri("/responses")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapToCustomException)
                .bodyToMono(OpenAiDto.OpenAiResponse.class)
                .retryWhen(retrySpecForTransientErrors())
                .block();

        return (res != null) ? res.extractText() : null;
    }


    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> req = OpenAiRequestFactory.chatRequest(model, systemPrompt, userPrompt);
        return openAiWebClient.post()
                .uri("/responses")
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapToCustomException)
                .bodyToMono(OpenAiDto.OpenAiResponse.class)
                .retryWhen(retrySpecForTransientErrors())
                .map(OpenAiDto.OpenAiResponse::extractText)
                .block();
    }

    public String analyzeFoodImageJsonText(String imageUrl, String prompt) {
        Map<String, Object> req = OpenAiRequestFactory.visionRequest(model, prompt, imageUrl);
        return responsesText(req);
    }

    private Mono<? extends Throwable> mapToCustomException(ClientResponse response) {
        int status = response.statusCode().value();

        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    if (status == 429) {
                        return new CustomException(ErrorCode.AI_RATE_LIMITED, body);
                    }
                    if (status == 401 || status == 403) {
                        return new CustomException(ErrorCode.AI_UPSTREAM_AUTH_FAILED, body);
                    }
                    if (status >= 400 && status <= 499) {
                        return new CustomException(ErrorCode.AI_INVALID_REQUEST, body);
                    }
                    if (status >= 500) {
                        return new CustomException(ErrorCode.AI_UPSTREAM_ERROR, body);
                    }
                    return new CustomException(ErrorCode.AI_UPSTREAM_ERROR, body);
                });
    }

    private Retry retrySpecForTransientErrors() {
        return Retry.backoff(2, Duration.ofMillis(500))
                .maxBackoff(Duration.ofSeconds(2))
                .filter(this::isTransientError)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isTransientError(Throwable t) {
        if (t instanceof CustomException ce) {
            return ce.getErrorCode() == ErrorCode.AI_UPSTREAM_ERROR;
        }
        return (t instanceof WebClientRequestException);
    }
}
