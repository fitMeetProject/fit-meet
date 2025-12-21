package com.ssafy.fitmeet.domain.user.service;

import com.ssafy.fitmeet.domain.user.dao.UserBodyInfoDao;
import com.ssafy.fitmeet.domain.user.dao.UserDao;
import com.ssafy.fitmeet.domain.user.dto.UserDto.ChangePasswordRequest;
import com.ssafy.fitmeet.domain.user.dto.UserDto.ProfileBodyInfoResponse;
import com.ssafy.fitmeet.domain.user.dto.UserDto.ProfileImageResponse;
import com.ssafy.fitmeet.domain.user.dto.UserDto.UpdateProfileRequest;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.domain.user.entity.UserBodyInfo;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import com.ssafy.fitmeet.global.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import static com.ssafy.fitmeet.global.util.PasswordValidator.validate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserDao userDao;
    private final UserBodyInfoDao userBodyInfoDao;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /**
     * 현재 로그인한 유저의 email 가져오기
     */
    @Override
    public String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    /**
     * 이메일 사용자 조회 ( 로그인, 이메일 중복 체크 )
     */
    @Override
    public User getUserInfoEmail(String email) {
    	User user = userDao.findByEmail(email);
    	if(user == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);
    	return user;
    }

    /**
     * ID로 사용자 조회 (커뮤니티 등에서 작성자 정보 조회용)
     */
    @Override
    public User getUserById(Long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * id 사용자 프로필 + 신체정보 조회
     */
    @Override
    public ProfileBodyInfoResponse getProfileBody() {
        String email = getCurrentUserEmail();
        User user = getUserInfoEmail(email);
        UserBodyInfo bodyInfo = findByUserId(user.getId());

        return ProfileBodyInfoResponse.of(user, bodyInfo);
    }

    /**
     * 신체 정보 조회
     */
    @Override
    public UserBodyInfo findByUserId(Long userId) {
        return userBodyInfoDao.findByUserId(userId);
    }
    
    /**
     * 신체 정보 등록 
     */
    @Override
    @Transactional
    public void insertBodyInfo(UserBodyInfo userBodyInfo) {
    	String email = getCurrentUserEmail();
    	User user = getUserInfoEmail(email);
        
        UserBodyInfo bodyInfo = findByUserId(user.getId());
    	userBodyInfo.setUserId(user.getId());

        // BMR / Daily Kcal 계산
        calculateBodyMetrics(userBodyInfo);
    	
        if(bodyInfo == null) {
            userBodyInfoDao.insertBodyInfo(userBodyInfo);
        } else {
        	userBodyInfoDao.updateBodyInfo(userBodyInfo);
        }
    }

    /**
     * 프로필 + 신체 정보 수정
     */
    @Override
    @Transactional
    public void updateProfile(UpdateProfileRequest request) {
    	String email = getCurrentUserEmail();
    	User user = getUserInfoEmail(email);

        // 닉네임 변경
        user.setNickname(request.nickname());
        user.setUpdatedAt(LocalDateTime.now());

        userDao.updateProfileBasic(user.getId(), request.nickname());

        UserBodyInfo bodyInfo = userBodyInfoDao.findByUserId(user.getId());
        if (bodyInfo == null) {
            bodyInfo = UserBodyInfo.builder()
                    .userId(user.getId())
                    .build();
            bodyInfo.setHeightCm(request.heightCm());
            bodyInfo.setWeightKg(request.weightKg());
            bodyInfo.setTargetWeightKg(request.targetWeightKg());
            bodyInfo.setGender(request.gender());
            bodyInfo.setBirthDate(request.birthDate());
            bodyInfo.setActivityLevel(request.activityLevel());
            bodyInfo.setCreatedAt(LocalDateTime.now());
            bodyInfo.setUpdatedAt(LocalDateTime.now());

            // 계산
            calculateBodyMetrics(bodyInfo);

            userBodyInfoDao.insertBodyInfo(bodyInfo);
        } else {
            bodyInfo.setHeightCm(request.heightCm());
            bodyInfo.setWeightKg(request.weightKg());
            bodyInfo.setTargetWeightKg(request.targetWeightKg());
            bodyInfo.setGender(request.gender());
            bodyInfo.setBirthDate(request.birthDate());
            bodyInfo.setActivityLevel(request.activityLevel());
            bodyInfo.setUpdatedAt(LocalDateTime.now());

            // 계산
            calculateBodyMetrics(bodyInfo);

            userBodyInfoDao.updateBodyInfo(bodyInfo);
        }
    }

    /**
     * 비밀번호 변경
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
    	String email = getCurrentUserEmail();
    	User user = getUserInfoEmail(email);

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.AUTH_BAD_CREDENTIALS);
        }

        validate(request.newPassword());

        String encoded = passwordEncoder.encode(request.newPassword());
        userDao.updatePassword(user.getId(), encoded, LocalDateTime.now());
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @Override
    @Transactional
    public void softDelete() {
    	String email = getCurrentUserEmail();
    	User user = getUserInfoEmail(email);

        userDao.softDelete(user.getId(), "WITHDRAWN", LocalDateTime.now());
    }

    /**
     * 목표 칼로리, BMR 계산 로직
     */
    private void calculateBodyMetrics(UserBodyInfo bodyInfo) {
        // 필수 정보가 없으면 계산하지 않음
        if (bodyInfo.getHeightCm() == null ||
                bodyInfo.getWeightKg() == null ||
                bodyInfo.getBirthDate() == null ||
                bodyInfo.getGender() == null) {
            return;
        }

        double height = bodyInfo.getHeightCm(); // cm
        double weight = bodyInfo.getWeightKg(); // kg
        LocalDate birthDate = bodyInfo.getBirthDate();
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        String gender = bodyInfo.getGender();

        // 1) BMR 계산 (Mifflin-St Jeor 공식)
        double bmr;
        if ("MALE".equalsIgnoreCase(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        } else if ("FEMALE".equalsIgnoreCase(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age;
        }

        // 2) 활동량에 따른 활동계수 (TDEE)
        String level = bodyInfo.getActivityLevel() != null ? bodyInfo.getActivityLevel().toUpperCase() : "NONE";

        double activityFactor;
        switch (level) {
            case "LOW":
                activityFactor = 1.375; // 가벼운 활동
                break;
            case "MID":
                activityFactor = 1.55;  // 보통 활동
                break;
            case "HIGH":
                activityFactor = 1.725; // 격한 활동
                break;
            case "NONE":
            default:
                activityFactor = 1.2;   // 거의 활동 없음
        }

        double tdee = bmr * activityFactor;

        // 3) 목표 체중에 따른 약간의 조정 (간단 버전)
        Double targetWeight = bodyInfo.getTargetWeightKg();
        if (targetWeight != null) {
            if (targetWeight < weight - 0.1) {
                // 감량 목표: 유지 칼로리에서 약간 마이너스
                tdee -= 300;
            } else if (targetWeight > weight + 0.1) {
                // 증량 목표: 유지 칼로리에서 약간 플러스
                tdee += 200;
            }
        }

        bodyInfo.setBmr((int) Math.round(bmr));
        bodyInfo.setDailyKcal((int) Math.round(tdee));
    }

    @Override
    public ProfileImageResponse getProfileImage() {
        String email = getCurrentUserEmail();
        User user = getUserInfoEmail(email);

        UserBodyInfo bodyInfo = userBodyInfoDao.findByUserId(user.getId());
        boolean hasBodyInfo = bodyInfo != null;

        return ProfileImageResponse.of(user, hasBodyInfo);
    }


    @Override
    @Transactional
    public String updateProfileImage(MultipartFile profileImage) {
        String email = getCurrentUserEmail();
        User user = getUserInfoEmail(email);

        String newUrl = fileStorageService.storeProfileImage(user.getId(), profileImage);

        fileStorageService.deleteProfileImageFile(user.getProfileImageUrl());

        userDao.updateProfileImage(user.getId(), newUrl);
        user.setProfileImageUrl(newUrl);

        return newUrl;
    }

    @Override
    @Transactional
    public void deleteProfileImage() {
        String email = getCurrentUserEmail();
        User user = getUserInfoEmail(email);

        fileStorageService.deleteProfileImageFile(user.getProfileImageUrl());

        userDao.updateProfileImage(user.getId(), null);
        user.setProfileImageUrl(null);
    }

}
