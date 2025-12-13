package com.ssafy.fitmeet.domain.user.service;

import com.ssafy.fitmeet.domain.user.dto.UserDto.*;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;

public interface UserService {

	/**
	 * 현재 로그인한 유저의 email 가져오기
	 */
	String getCurrentUserEmail();

	/**
	 * 이메일 사용자 조회 ( 로그인, 이메일 중복 체크 )
	 */
	User getUserInfoEmail(String email);

	/**
	 * ID로 사용자 조회 (커뮤니티 등에서 작성자 정보 조회용)
	 */
	User getUserById(Long userId);

	/**
	 * id 사용자 프로필 + 신체정보 조회
	 */
	ProfileBodyInfoResponse getProfileBody();

	/**
	 * 신체 정보 조회
	 */
	UserBodyInfo findByUserId(Long userId);

	/**
	 * 신체 정보 등록 
	 */
	void insertBodyInfo(UserBodyInfo userBodyInfo);

	/**
	 * 프로필 + 신체 정보 수정
	 */
	void updateProfile(UpdateProfileRequest request);

	/**
	 * 비밀번호 변경
	 */
	void changePassword(ChangePasswordRequest request);

	/**
	 * 회원 탈퇴 (소프트 삭제)
	 */
	void softDelete();

	// ★ 프로필 이미지 관련
	ProfileImageResponse getProfileImage(); // 조회
	String updateProfileImage(org.springframework.web.multipart.MultipartFile profileImage); // 수정(업로드/변경)
	void deleteProfileImage();
}
