package com.ssafy.fitmeet.global.config.ai;

import com.ssafy.fitmeet.global.config.ai.OpenAiChatDto.OpenAiChatRequest;
import com.ssafy.fitmeet.global.config.ai.OpenAiChatDto.OpenAiChatResponse;
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

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::mapToCustomException)
                .bodyToMono(OpenAiChatResponse.class)
                .retryWhen(retrySpecForTransientErrors())
                .map(res -> res.getChoices().get(0).getMessage().getContent())
                .block();
    }

    private Mono<? extends Throwable> mapToCustomException(ClientResponse response) {
        int status = response.statusCode().value();

        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    // 429: 사용자에게 즉시 안내(에러)
                    if (status == 429) {
                        return new CustomException(ErrorCode.AI_RATE_LIMITED, body);
                    }

                    // 401/403: 설정/인증 문제(운영 이슈) -> 에러
                    if (status == 401 || status == 403) {
                        return new CustomException(ErrorCode.AI_UPSTREAM_AUTH_FAILED, body);
                    }

                    // 400계열: 우리가 잘못 보낸 요청(개발 버그 가능) -> 에러
                    if (status >= 400 && status <= 499) {
                        return new CustomException(ErrorCode.AI_INVALID_REQUEST, body);
                    }

                    // 500계열: upstream 오류 -> 일단 예외로 던짐(서비스에서 fallback 가능)
                    if (status >= 500) {
                        return new CustomException(ErrorCode.AI_UPSTREAM_ERROR, body);
                    }

                    return new CustomException(ErrorCode.AI_UPSTREAM_ERROR, body);
                });
    }

    private Retry retrySpecForTransientErrors() {
        return Retry.backoff(2, Duration.ofMillis(500)) // 최대 2회
                .maxBackoff(Duration.ofSeconds(2))
                .filter(this::isTransientError)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean isTransientError(Throwable t) {
        // 5xx만 재시도
        if (t instanceof CustomException ce) {
            ErrorCode code = ce.getErrorCode();
            return code == ErrorCode.AI_UPSTREAM_ERROR;
        }

        // 네트워크/타임아웃(연결 실패 등)
        return (t instanceof WebClientRequestException);
    }
}
