package com.ssafy.fitmeet.support.seed.demo;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoSeedPlan {

    private Long demoUserId;

    // 기간
    private int daysRange; // 최근 N일

    // Diet
    private int dietPerDayMin;
    private int dietPerDayMax;

    // Workout history
    private int workoutDaysRate; // 0~100 (데모는 보통 높게)
    private int workoutPerDayMin;
    private int workoutPerDayMax;

    // Video likes (demo user가 좋아요 누른 영상 수)
    private int demoVideoLikeMin;
    private int demoVideoLikeMax;

    // Community
    private int communityPosts;
    private int communityLikeMaxPerPost;
    private int communityCommentMaxPerPost;
    private int communityCommentDeleteRate;

    public static DemoSeedPlan defaultPlan() {
        return DemoSeedPlan.builder()
                .demoUserId(1L)
                .daysRange(30)
                .dietPerDayMin(2)
                .dietPerDayMax(3)
                .workoutDaysRate(70)
                .workoutPerDayMin(1)
                .workoutPerDayMax(2)
                .demoVideoLikeMin(25)
                .demoVideoLikeMax(45)
                .communityPosts(25)
                .communityLikeMaxPerPost(20)
                .communityCommentMaxPerPost(8)
                .communityCommentDeleteRate(10)
                .build();
    }
}
