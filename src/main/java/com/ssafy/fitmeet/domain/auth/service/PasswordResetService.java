package com.ssafy.fitmeet.domain.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ssafy.fitmeet.global.util.PasswordValidator.validate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.fitmeet.domain.auth.dao.PasswordResetTokenDao;
import com.ssafy.fitmeet.domain.auth.entity.PasswordResetToken;
import com.ssafy.fitmeet.domain.user.dao.UserDao;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserDao userDao;
    private final PasswordResetTokenDao passwordResetTokenDao;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final long EXPIRE_MINUTES = 30L;

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userDao.findByEmail(email);

        if (user == null) {
            return;
        }

        passwordResetTokenDao.markAllAsUsedByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        passwordResetTokenDao.insert(token);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
    }


    @Transactional
    public void resetPassword(String token, String newPassword) {
        validate(newPassword);
        PasswordResetToken prt = passwordResetTokenDao.findByToken(token);

        if (prt == null) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID);
        }
        if (prt.isExpired()) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_EXPIRED);
        }
        if (prt.isUsed()) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_USED);
        }

        User user = userDao.findById(prt.getUserId());
        if (user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String encoded = passwordEncoder.encode(newPassword);
        userDao.updatePassword(user.getId(), encoded, LocalDateTime.now());

        passwordResetTokenDao.markAsUsed(prt.getId());
    }

    @Transactional(readOnly = true)
    public void validateToken(String tokenValue) {
        PasswordResetToken token = passwordResetTokenDao.findByToken(tokenValue);

        if (token == null) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID);
        }
        if (token.isExpired()) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_EXPIRED);
        }
        if (token.isUsed()) {
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_USED);
        }
    }

}
