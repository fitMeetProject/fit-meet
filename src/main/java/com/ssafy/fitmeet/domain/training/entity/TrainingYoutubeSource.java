package com.ssafy.fitmeet.domain.training.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingYoutubeSource {
    private Long id;
    private String sourceType; // KEYWORD / PLAYLIST
    private String sourceValue; // 검색어
    private String mainCategory; // YOGA / PILATES / HOME ...
    private String level; // BEGINNER / INTERMEDIATE / ADVANCED
    private Integer maxResults;
    private Integer isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
