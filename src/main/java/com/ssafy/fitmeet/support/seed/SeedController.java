package com.ssafy.fitmeet.support.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/seed")
public class SeedController {

    private final SeedService seedService;

    @PostMapping("/run")
    public ResponseEntity<?> run(
            @RequestHeader(name = "X-SEED-SECRET", required = false) String secret,
            @RequestBody(required = false) SeedPlan plan
    ) {
        seedService.runOnce(secret, plan == null ? SeedPlan.defaultPlan() : plan);
        return ResponseEntity.ok("OK");
    }
}
