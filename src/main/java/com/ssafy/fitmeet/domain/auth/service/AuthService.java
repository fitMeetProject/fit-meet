package com.ssafy.fitmeet.domain.auth.service;

import com.ssafy.fitmeet.domain.auth.dto.AuthDto.LoginRequest;
import com.ssafy.fitmeet.domain.auth.dto.AuthDto.LoginResponse;
import com.ssafy.fitmeet.domain.auth.dto.AuthDto.SignUpRequest;
import com.ssafy.fitmeet.domain.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

	/**
	 * 회원가입
	 */
	Long signUp(SignUpRequest request, MultipartFile profileImage);


	/**
	 * 이메일 사용자 조회 ( 로그인, 이메일 중복 체크 )
	 */
	User getUserInfoEmail(String email);

	/**
	 * 로그인 + JWT 발급
	 */
	LoginResponse login(LoginRequest request);

	/**
	 * 리프레시 메서드
	 */
	String regenerateAccessTokenByRefreshToken(String refreshToken);

	/**
	 * 로그아웃 메서드
	 */
	void logout(String email);

}
