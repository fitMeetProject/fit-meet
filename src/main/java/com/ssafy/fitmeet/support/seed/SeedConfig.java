package com.ssafy.fitmeet.support.seed;

import net.datafaker.Faker;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration
public class SeedConfig {

    @Bean
    public Faker faker() {
        return new Faker(new Locale("ko"), new Random(42)); // 재현성
    }
}
