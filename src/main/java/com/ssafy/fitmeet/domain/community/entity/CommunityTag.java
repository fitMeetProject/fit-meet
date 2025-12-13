package com.ssafy.fitmeet.domain.community.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityTag {

    private Long id;
    private String name;
    private int usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

