package com.ssafy.fitmeet.support.seed;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SeedPlan {
    private int users = 1000;
    private int meals = 1000;
    private int diets = 1000;
    private int dietDaysRange = 14; // 최근 14일 범위
    private int bodyInfoRate = 70;

    // dashboard
    private int dashboardDaysRange = 14;

    // AI weekly log
    private int aiWeeksBack = 8;
    private int aiLogsPerUserMax = 1;

    // Training
    private int workoutDaysRange = 14;
    private int workoutDaysRate = 40; // 날짜별 운동할 확률 40%
    private int workoutPerDayMin = 1;
    private int workoutPerDayMax = 2;

    public static SeedPlan defaultPlan() {
        return new SeedPlan();
    }
}
