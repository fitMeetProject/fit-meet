package com.ssafy.fitmeet.domain.auth.service;

import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url.front-base}")
    private String frontBaseUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontBaseUrl + "/password-reset?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[FitMeet] 비밀번호 재설정 안내");
        message.setText(
                "안녕하세요, FitMeet 입니다.\n\n" +
                        "아래 링크를 클릭하여 비밀번호를 재설정해주세요.\n" +
                        resetLink + "\n\n" +
                        "본 링크는 30분 동안만 유효합니다.\n" +
                        "본인이 요청하지 않은 경우 이 메일을 무시해 주세요."
        );

        try {
            mailSender.send(message);
            System.out.println("메일 발송 시도 성공");
        } catch (Exception e) {
            System.out.println("메일 발송 중 예외 발생: " + e.getClass().getName());
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_EMAIL_NOT_FOUND);
        }
    }
}
