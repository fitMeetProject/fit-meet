package com.ssafy.fitmeet.global.config.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class OpenAiDto {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAiResponse {
        private List<Output> output;

        public String extractText() {
            if (output == null) return null;

            return output.stream()
                    .filter(o -> o.getContent() != null)
                    .flatMap(o -> o.getContent().stream())
                    .filter(c -> "output_text".equals(c.getType()))
                    .map(Content::getText)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Output {
        private List<Content> content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private String type;
        private String text;
    }

    @Data @AllArgsConstructor
    public static class VisionRequest {
        private String model;
        private List<Message> messages;
        private ResponseFormat response_format;

        @Data @AllArgsConstructor
        public static class Message {
            private String role;
            private List<Content> content;
        }

        public sealed interface Content permits TextContent, ImageUrlContent {}

        @Data @AllArgsConstructor
        public static final class TextContent implements Content {
            private String type;
            private String text;
        }

        @Data @AllArgsConstructor
        public static final class ImageUrlContent implements Content {
            private String type;
            private ImageUrl image_url;

            @Data @AllArgsConstructor
            public static class ImageUrl {
                private String url;
            }
        }

        @Data @AllArgsConstructor
        public static class ResponseFormat {
            private String type;
            private JsonSchema json_schema;

            @Data @AllArgsConstructor
            public static class JsonSchema {
                private String name;
                private Schema schema;

                @Data @AllArgsConstructor
                public static class Schema {
                    private String type; // "object"
                    private Map<String, Object> properties;
                    private List<String> required;
                    private boolean additionalProperties;
                }
            }
        }
    }

    public record FoodCandidate(
            String nameKo,
            String nameEn,
            double confidence
    ) {}

    public record FoodAnalyzeResponse(
            List<FoodCandidate> items,
            boolean needsUserPortion,
            String notes
    ) {}
}
