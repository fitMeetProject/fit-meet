package com.ssafy.fitmeet.domain.meal.dao;

import com.ssafy.fitmeet.domain.meal.entity.Meal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealDao {

    void insert(Meal meal);

    int insertMany(@Param("list") List<Meal> list);

    List<Long> findIdsByFoodCdIn(@Param("list") List<String> foodCds);

    Meal findById(@Param("id") Long id);

    Meal findByFoodCd(@Param("foodCd") String foodCd);

    List<Meal> searchByKeyword(@Param("keyword") String keyword, @Param("category") String category);

    List<Long> findAllIds();

}
