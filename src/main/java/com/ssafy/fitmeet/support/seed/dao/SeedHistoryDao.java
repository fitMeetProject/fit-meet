package com.ssafy.fitmeet.support.seed.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SeedHistoryDao {
    int insert(@Param("name") String name);
}
