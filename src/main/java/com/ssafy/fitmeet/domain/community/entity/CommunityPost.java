package com.ssafy.fitmeet.domain.community.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;

    private LocalDate activityDate;
    private boolean shareDiet;
    private boolean shareStreak;
    private boolean shareWorkout;
    private String workoutNote;
    private int workoutVideoId;
    private String workoutVideoUrl;

    private int likeCount;
    private int viewCount;
    private int commentCount;

    private boolean isDeleted;
    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
