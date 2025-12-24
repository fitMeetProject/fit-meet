package com.ssafy.fitmeet.support.seed;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedPlan {
    private int users;
    private int meals;
    private int diets;

    private int bodyInfoRate;
    private int dietDaysRange;
    private int dashboardDaysRange;

    private int aiWeeksBack;
    private int aiLogsPerUserMax;

    private int workoutDaysRange;
    private int workoutDaysRate;
    private int workoutPerDayMin;
    private int workoutPerDayMax;

    private int perVideoViewMin;
    private int perVideoViewMax;
    private int perVideoLikeMin;
    private int perVideoLikeMax;
    private int perVideoCommentMin;
    private int perVideoCommentMax;
    private int videoCommentDeleteRate;

    private int communityPosts;
    private int communityDaysRange;
    private int communityLikeMaxPerPost;
    private int communityCommentMaxPerPost;
    private int communityCommentDeleteRate;

    public static SeedPlan defaultPlan() {
        return SeedPlan.builder()
                .users(30)
                .meals(300)
                .diets(500)
                .bodyInfoRate(70)
                .dietDaysRange(30)
                .dashboardDaysRange(30)
                .aiWeeksBack(8)
                .aiLogsPerUserMax(3)
                .workoutDaysRange(30)
                .workoutDaysRate(40)
                .workoutPerDayMin(1)
                .workoutPerDayMax(3)
                .perVideoViewMin(0)
                .perVideoViewMax(300)
                .perVideoLikeMin(0)
                .perVideoLikeMax(30)
                .perVideoCommentMin(0)
                .perVideoCommentMax(20)
                .videoCommentDeleteRate(10)
                .communityPosts(200)
                .communityDaysRange(30)
                .communityLikeMaxPerPost(25)
                .communityCommentMaxPerPost(8)
                .communityCommentDeleteRate(15)
                .build();
    }
}
