package com.ssafy.fitmeet.domain.community.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private String postImageUrl;

    private LocalDate activityDate;
    private boolean shareDiet;
    private boolean shareStreak;
    private boolean shareWorkout;
    private String dietNote;
    private String workoutNote;

    private int likeCount;
    private int viewCount;
    private int commentCount;

    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
