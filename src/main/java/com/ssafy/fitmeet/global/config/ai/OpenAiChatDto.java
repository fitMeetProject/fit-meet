package com.ssafy.fitmeet.global.config.ai;

import lombok.*;
import java.util.List;

public class OpenAiChatDto {

    @Data
    @AllArgsConstructor
    public static class OpenAiChatRequest {
        private String model;
        private List<Message> messages;

        @Data
        @AllArgsConstructor
        public static class Message {
            private String role; // system / user
            private String content;
        }
    }

    @Data
    public static class OpenAiChatResponse {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private Message message;
        }

        @Data
        public static class Message {
            private String role;
            private String content;
        }
    }
}
