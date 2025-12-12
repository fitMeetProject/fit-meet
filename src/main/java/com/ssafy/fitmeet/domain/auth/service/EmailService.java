package com.ssafy.fitmeet.domain.auth.service;

import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url.front-base}")
    private String frontBaseUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        // 1. 재설정 링크 생성
        String resetLink = frontBaseUrl +"/auth"+ "/password-reset?token=" + token;

        // 2. MimeMessage 객체 생성
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            // 3. MimeMessageHelper를 사용하여 HTML 콘텐츠 설정 준비
            // true: 멀티파트 메시지 허용 (인코딩: utf-8)
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("[FitMeet] 비밀번호 재설정 안내");

            // 4. HTML 콘텐츠 생성 및 설정
            String htmlContent = buildHtmlContent(resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            System.out.println("메일 발송 시도 성공: HTML 형식");

        } catch (MessagingException e) {
            // MimeMessageHelper 관련 예외 처리
            System.out.println("메일 설정/발송 중 예외 발생 (MessagingException): " + e.getMessage());
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_EMAIL_NOT_FOUND);
        } catch (Exception e) {
            // 기타 예외 처리
            System.out.println("메일 발송 중 예외 발생: " + e.getClass().getName());
            throw new CustomException(ErrorCode.AUTH_PASSWORD_RESET_EMAIL_NOT_FOUND);
        }
    }

    /**
     * 실제로 전송될 HTML 내용을 구성하는 헬퍼 메서드
     */
    private String buildHtmlContent(String resetLink) {
        // 인라인 CSS 스타일 정의
        String buttonStyle =
                "display: inline-block; " +
                        "padding: 12px 25px; " +
                        "margin: 20px 0; " +
                        "font-size: 16px; " +
                        "color: white !important; " + // !important로 일부 메일 클라이언트의 스타일 오버라이드를 방지
                        "background-color: #D3A373; " + // FitMeet의 주 색상
                        "border-radius: 8px; " +
                        "text-decoration: none; " + // 링크 밑줄 제거
                        "font-weight: bold; " +
                        "text-align: center;";

        String containerStyle =
                "border: 1px solid #eee; " +
                        "padding: 30px; " +
                        "max-width: 600px; " +
                        "margin: 20px auto; " +
                        "font-family: Arial, sans-serif; " +
                        "line-height: 1.6;";

        return
                "<html>" +
                        "<body style='margin: 0; padding: 0; background-color: #f4f4f4;'>" +
                        "<div style='" + containerStyle + " background-color: white;'>" +
                        "<h2 style='color: #8A8F6E; border-bottom: 2px solid #eee; padding-bottom: 10px;'>FitMeet 비밀번호 재설정</h2>" +
                        "<p>안녕하세요. 비밀번호 재설정을 요청해 주셨습니다.</p>" +
                        "<p>아래 버튼을 클릭하여 새로운 비밀번호를 설정해주세요.</p>" +

                        // 버튼 형태의 링크 (핵심: a 태그에 인라인 스타일 적용)
                        "<table role='presentation' border='0' cellpadding='0' cellspacing='0'>" +
                        "<tr>" +
                        "<td align='center'>" +
                        "<a href='" + resetLink + "' style='" + buttonStyle + "'>" +
                        "비밀번호 재설정하기" +
                        "</a>" +
                        "</td>" +
                        "</tr>" +
                        "</table>" +

                        "<p style='margin-top: 30px; font-size: 12px; color: #888;'>" +
                        "만약 버튼이 작동하지 않는다면, 아래 링크를 복사하여 브라우저에 붙여넣어 주세요.<br>" +
                        "<a href='" + resetLink + "' style='color: #D3A373; word-break: break-all;'>" + resetLink + "</a>" +
                        "</p>" +
                        "<hr style='border: 0; border-top: 1px solid #eee; margin-top: 30px;'>" +
                        "<p style='font-size: 14px; color: #888;'>" +
                        "⚠️ 본 링크는 보안을 위해 **30분** 동안만 유효합니다.<br>" +
                        "본인이 요청하지 않은 경우 이 메일을 무시해 주세요." +
                        "</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>";
    }
}
