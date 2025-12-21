package com.ssafy.fitmeet.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.fitmeet.domain.dashboard.dto.DashboardDto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeeklyAiResultParser {
    private final ObjectMapper objectMapper;

    public WeeklyAiResult parseOrNull(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) return null;
        try {
            return objectMapper.readValue(responseJson, WeeklyAiResult.class);
        } catch (Exception e) {
            return null;
        }
    }
}
