package com.ssafy.fitmeet.support.seed.util;

import net.datafaker.Faker;

import java.util.*;

public final class SeedPickUtils {

    private SeedPickUtils() {}

    public static <T> T pickOne(Faker faker, List<T> list) {
        return list.get(faker.number().numberBetween(0, list.size()));
    }

    public static <T> List<T> pickDistinct(Faker faker, List<T> list, int count) {
        if (list == null || list.isEmpty()) return List.of();
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, new Random(faker.random().nextInt()));
        return copy.subList(0, Math.min(count, copy.size()));
    }

    public static <T> Set<T> pickDistinctSet(Faker faker, List<T> list, int count) {
        int n = Math.min(count, list.size());
        Set<T> picked = new LinkedHashSet<>(n);
        while (picked.size() < n) {
            picked.add(list.get(faker.number().numberBetween(0, list.size())));
        }
        return picked;
    }
}
