package com.ssafy.fitmeet.domain.auth.web;

import com.ssafy.fitmeet.domain.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/password-reset")
@Tag(name = "PasswordReset", description = "비밀번호 재설정 관련")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    @Operation(summary = "비밀번호 재설정 메일 발송", description = "비밀번호 재설정 메일 발송 ( 토큰 30분 유효 )")
    public ResponseEntity<?> request(@RequestBody RequestDto dto) {
        passwordResetService.requestPasswordReset(dto.email());
        return ResponseEntity.ok("비밀번호 재설정 메일이 발송되었습니다.");
    }

    @PostMapping("/confirm")
    @Operation(summary = "비밀번호 변경", description = "비밀번호 변경 API ( token, newPassword )")
    public ResponseEntity<?> confirm(@RequestBody ConfirmDto dto) {
        passwordResetService.resetPassword(dto.token(), dto.newPassword());
        return ResponseEntity.ok("비밀번호가 변경되었습니다.");
    }

    @GetMapping("/validate")
    @Operation(summary = "비밀번호 유효성 검사", description = "최소 1개 영문자, 최소 1개 숫자, 최소 1개 특수문자로 구성된 8자에서 20자 비밀번호")
    public ResponseEntity<?> validate(@RequestParam String token) {
        passwordResetService.validateToken(token);
        return ResponseEntity.ok("VALID");
    }

    public record RequestDto(String email) {}
    public record ConfirmDto(String token, String newPassword) {}
}
