package com.ssafy.fitmeet.global.file;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;

@Service
@Log4j2
public class FileStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 프로필 이미지 저장 후 접근 가능한 URL 반환
     */
    public String storeProfileImage(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf(".")).toLowerCase();
        }

        // 확장자 제한 (원하면)
        if (!ext.matches("\\.(png|jpg|jpeg|webp)$")) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }

        try {
            Path profileDir = Paths.get(uploadDir, "profile");
            Files.createDirectories(profileDir);

            String filename = "user_" + userId + "_" + UUID.randomUUID() + ext;
            Path targetPath = profileDir.resolve(filename).normalize();

            file.transferTo(targetPath.toFile());

            return baseUrl + "/static/profile/" + filename;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAIL);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    /**
     * 기존 프로필 이미지 파일 삭제 (URL 기준)
     */
    public void deleteProfileImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return; // 삭제할 파일 없음
        }

        try {
            int idx = imageUrl.lastIndexOf("/static/");
            if (idx == -1) {
                return;
            }

            String relativePath = imageUrl.substring(idx + "/static/".length());
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);

        } catch (IOException e) {
             log.warn("프로필 이미지 파일 삭제 실패: {}", imageUrl, e);
        }
    }
}
