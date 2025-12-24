package com.ssafy.fitmeet.support.seed;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/seed")
@Tag(name = "Seeding", description = "더미데이터 심기")
public class SeedController {

    private final SeedService seedService;

    @PostMapping("/run")
    @Operation(summary = "시딩 run", description = "시딩 실행하기 전에 디비 클린하고 데모 계정 무조건 하나 만들고, 신체정보 추가하고, Youtube 싱크 맞추고 시작 ( !!!! 중요 !!!! )")
    public ResponseEntity<?> run(
            @RequestHeader(name = "X-SEED-SECRET", required = false) String secret,
            @RequestBody(required = false) SeedPlan plan
    ) {
        seedService.runOnce(secret, plan == null ? SeedPlan.defaultPlan() : plan);
        return ResponseEntity.ok("OK");
    }
}
