package com.ssafy.fitmeet.support.seed.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SeedFileProvisioner {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * classpath 리소스(예: src/main/resources/seed-images/profile/test*.jpg)를
     * 실제 업로드 디렉토리(uploadDir/profile)에 복사하고, 접근 URL 리스트를 반환.
     *
     * @param classpathDir 예: "seed-images/profile"
     */
    public List<String> provisionProfileImages(String classpathDir) {
        return provision(classpathDir, Paths.get(uploadDir, "profile"), baseUrl + "/static/profile/");
    }

    /**
     * 커뮤니티 게시글 이미지
     * @param classpathDir 예: "seed-images/community-post"
     */
    public List<String> provisionCommunityImages(String classpathDir) {
        return provision(classpathDir, Paths.get(uploadDir, "community", "post"), baseUrl + "/static/community/post/");
    }

    private List<String> provision(String classpathDir, Path targetDir, String urlPrefix) {
        try {
            Files.createDirectories(targetDir.toAbsolutePath().normalize());

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // JAR에서도 동작하도록 classpath* 패턴 사용
            Resource[] resources = resolver.getResources("classpath*:" + classpathDir + "/*.*");

            List<String> urls = new ArrayList<>();
            for (Resource r : resources) {
                if (!r.exists() || !r.isReadable()) continue;

                String original = r.getFilename();
                if (original == null) continue;

                String lower = original.toLowerCase();
                if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))) {
                    continue;
                }

                // 충돌 방지: UUID prefix
                String filename = "seed_" + UUID.randomUUID() + "_" + original;
                Path out = targetDir.resolve(filename).normalize();

                Files.copy(r.getInputStream(), out, StandardCopyOption.REPLACE_EXISTING);
                urls.add(urlPrefix + filename);
            }
            return urls;

        } catch (Exception e) {
            // 시딩 도중 이미지 때문에 전체 실패는 부담 → 빈 리스트 반환
            return List.of();
        }
    }
}
