package com.ssafy.fitmeet.domain.meal.dao;

import com.ssafy.fitmeet.domain.meal.entity.MealNutrition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealNutritionDao {

    void insert(MealNutrition nutrition);

    int insertMany(@Param("list") List<MealNutrition> list);

    MealNutrition findByMealId(@Param("mealId") Long mealId);

}
