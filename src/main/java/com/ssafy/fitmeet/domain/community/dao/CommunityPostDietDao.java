package com.ssafy.fitmeet.domain.community.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityPostDietDao {

    void insert(@Param("postId") Long postId, @Param("dietId") Long dietId);

    void deleteByPostId(@Param("postId") Long postId);

    List<Long> findDietIdsByPostId(@Param("postId") Long postId);

    List<DietJoinRow> findDietCardsByPostId(@Param("postId") Long postId);

    record DietJoinRow(
            Long dietId,
            java.time.LocalDateTime date,
            String mealType,
            String description,
            String imageUrl,
            java.math.BigDecimal totalKcal,
            java.math.BigDecimal totalCarbo,
            java.math.BigDecimal totalProtein,
            java.math.BigDecimal totalFat
    ) {}

}
