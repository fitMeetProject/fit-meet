package com.ssafy.fitmeet.domain.auth.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expiresAt;
    private boolean used;
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void markUsed() {
        this.used = true;
    }

}
