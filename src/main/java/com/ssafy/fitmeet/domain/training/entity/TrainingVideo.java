package com.ssafy.fitmeet.domain.training.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingVideo {

    private Long id;
    private String youtubeVideoId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private Integer durationSec;
    private String mainCategory;
    private String level;
    private String tags;
    // 앱 내부 값
    private Long viewCount;
    private Long likeCount;
    // YouTube 통계
    private Long youtubeViewCnt;
    private Long youtubeLikeCnt;
    private LocalDateTime publishedAt;
    private Integer isRecommended;
    private Integer isActive;
    private String channelTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

