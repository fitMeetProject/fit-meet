package com.ssafy.fitmeet.domain.auth.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.fitmeet.domain.auth.entity.PasswordResetToken;

@Mapper
public interface PasswordResetTokenDao {

    // 토큰 문자열로 조회 (비밀번호 변경 시 사용)
    PasswordResetToken findByToken(@Param("token") String token);

    // 새 토큰 생성
    void insert(PasswordResetToken token);

    // 특정 토큰을 사용완료 처리
    int markAsUsed(@Param("id") Long id);

    // 특정 유저의 모든 미사용 토큰을 사용완료 처리 (새 토큰 발급 전 기존 것 무효화 용도)
    int markAllAsUsedByUserId(@Param("userId") Long userId);

    // 만료된 토큰 정리
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    // 특정 유저의 모든 토큰 삭제
    int deleteByUserId(@Param("userId") Long userId);
}
