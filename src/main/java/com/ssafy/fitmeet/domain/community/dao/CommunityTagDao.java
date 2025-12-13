package com.ssafy.fitmeet.domain.community.dao;

import com.ssafy.fitmeet.domain.community.entity.CommunityTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityTagDao {

    CommunityTag findByName(@Param("name") String name);

    void insert(CommunityTag tag);

    void increaseUsageCount(@Param("tagId") Long tagId, @Param("delta") int delta);

    List<CommunityTag> findTopTags(@Param("limit") int limit);
}

