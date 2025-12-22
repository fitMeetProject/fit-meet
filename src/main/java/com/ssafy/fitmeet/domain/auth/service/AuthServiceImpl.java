package com.ssafy.fitmeet.domain.auth.service;

import com.ssafy.fitmeet.domain.auth.dao.UserRefreshTokenDao;
import com.ssafy.fitmeet.domain.auth.entity.UserRefreshToken;
import com.ssafy.fitmeet.domain.user.dao.UserDao;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import com.ssafy.fitmeet.global.file.FileStorageService;
import com.ssafy.fitmeet.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static com.ssafy.fitmeet.domain.auth.dto.AuthDto.*;
import static com.ssafy.fitmeet.global.util.PasswordValidator.validate;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRefreshTokenDao userRefreshTokenDao;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public Long signUp(SignUpRequest request, MultipartFile profileImage) {
        // 이메일 중복 체크
        User existing = userDao.findByEmail(request.getEmail());
        if (existing != null) {
            throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATED);
        }

        validate(request.getPassword());

        // 1) 우선 유저 기본 정보로 insert
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .status("ACTIVE")
                .role("ROLE_USER")
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userDao.insertUser(user); // PK(id) 세팅됨

        // 2) 프로필 이미지가 있다면 로컬 저장 후 URL 업데이트
        if (profileImage != null && !profileImage.isEmpty()) {
            String url = fileStorageService.storeProfileImage(user.getId(), profileImage);
            userDao.updateProfileImage(user.getId(), url);
        }

        return user.getId();
    }

    @Override
    public User getUserInfoEmail(String email) {
        User user = userDao.findByEmail(email);
        if(user != null) throw new CustomException(ErrorCode.USER_EMAIL_DUPLICATED);
        return null;
    }

    /**
     * 로그인 + JWT 발급
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        com.ssafy.fitmeet.global.security.CustomUserDetails principal =
                (com.ssafy.fitmeet.global.security.CustomUserDetails) authentication.getPrincipal();

        String email = principal.getUsername();
        User user = userDao.findByEmail(email);
        if (user == null) {
        	throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_USER");

        String accessToken = jwtTokenProvider.createAccessToken(email, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        long refreshValidityMs = jwtTokenProvider.getRefreshTokenValidityInMs();
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusNanos(refreshValidityMs * 1_000_000);

        UserRefreshToken stored = userRefreshTokenDao.findByUserId(user.getId());
        if (stored == null) {
            stored = new UserRefreshToken();
            stored.setUserId(user.getId());
            stored.setRefreshToken(refreshToken);
            stored.setExpiresAt(refreshExpiresAt);
            userRefreshTokenDao.insert(stored);
        } else {
            stored.setRefreshToken(refreshToken);
            stored.setExpiresAt(refreshExpiresAt);
            userRefreshTokenDao.update(stored);
        }

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                LocalDateTime.now(),
                null
        );
    }

    /**
     * 리프레시 메서드
     */
    @Override
    @Transactional
    public String regenerateAccessTokenByRefreshToken(String refreshToken) {
        // 1. JWT 서명/만료 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
        	throw new CustomException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰에서 이메일 추출
        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userDao.findByEmail(email);
        if (user == null) {
        	throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 3. DB에 저장된 리프레시 토큰 조회
        UserRefreshToken stored = userRefreshTokenDao.findByUserId(user.getId());
        if (stored == null) {
            throw new CustomException(ErrorCode.AUTH_NON_SAVED_REFRESH_TOKEN);
        }

        // 4. DB에 저장된 토큰과 일치 여부 확인
        if (!stored.getRefreshToken().equals(refreshToken)) {
        	throw new CustomException(ErrorCode.AUTH_INVALID_REFRESH_TOKEN);
        }

        // 5. 만료 시간 확인
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.AUTH_EXPIRED_TOKEN);
        }

        // 6. 새 액세스 토큰 발급 (리프레시는 그대로 사용)
        return jwtTokenProvider.createAccessToken(email, user.getRole());
    }

	/**
	 * 로그아웃 메서드
	 */
    @Override
    @Transactional
    public void logout(String email) {
        User user = userDao.findByEmail(email);
        if (user == null) {
            return;
        }
        userRefreshTokenDao.deleteByUserId(user.getId());
    }
}
