/* =========================================================
   FitMeet - DDL (Workbench one-shot 실행용)
   - FK 의존성 고려한 생성 순서
========================================================= */

SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS fitmeet;
CREATE DATABASE IF NOT EXISTS fitmeet;
USE fitmeet;

/* =========================================================
   1) DROP TABLES (의존성 고려, 혹은 FK 체크 OFF 상태로 안전)
========================================================= */

DROP TABLE IF EXISTS seed_history;

-- AI / Dashboard
DROP TABLE IF EXISTS diet_ai_weekly_analysis_log;
DROP TABLE IF EXISTS user_daily_activity;
DROP TABLE IF EXISTS user_daily_nutrition;
DROP TABLE IF EXISTS user_daily_score;

-- Training (자식부터)
DROP TABLE IF EXISTS training_video_comment;
DROP TABLE IF EXISTS training_video_history;
DROP TABLE IF EXISTS training_video_like;
DROP TABLE IF EXISTS training_youtube_source;
DROP TABLE IF EXISTS training_video;

-- Community (자식부터)
DROP TABLE IF EXISTS community_comment;
DROP TABLE IF EXISTS community_post_like;
DROP TABLE IF EXISTS community_post_tag;
DROP TABLE IF EXISTS community_tag;
DROP TABLE IF EXISTS community_post_video;
DROP TABLE IF EXISTS community_post_diet;
DROP TABLE IF EXISTS community_post;

-- Diet (자식부터)
DROP TABLE IF EXISTS diet_info;
DROP TABLE IF EXISTS diet_score;
DROP TABLE IF EXISTS diet;

-- Meal master (자식부터)
DROP TABLE IF EXISTS meal_nutrition;
DROP TABLE IF EXISTS meal;

-- Auth/User (자식부터)
DROP TABLE IF EXISTS user_refresh_token;
DROP TABLE IF EXISTS password_reset_token;
DROP TABLE IF EXISTS user_body_info;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;


/* =========================================================
   2) CREATE TABLES (부모 → 자식 순)
========================================================= */

-- 2-1. USER (부모)
CREATE TABLE `user` (
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        `email` VARCHAR(100) NOT NULL,
                        `password` VARCHAR(255) NOT NULL,
                        `name` VARCHAR(50) NOT NULL,
                        `nickname` VARCHAR(50) NOT NULL,
                        `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                        `role` VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
                        `profile_image_url` VARCHAR(255) NULL,
                        `email_verified` TINYINT(1) NOT NULL DEFAULT 0,
                        `last_login_at` DATETIME NULL,
                        `password_changed_at` DATETIME NULL,
                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` DATETIME NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_email` (`email`),
                        KEY `idx_user_status` (`status`),
                        KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 2-2. USER 하위
CREATE TABLE `user_body_info` (
                                  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  `user_id` BIGINT UNSIGNED NOT NULL,
                                  `height_cm` DECIMAL(5,2) NULL,
                                  `weight_kg` DECIMAL(5,2) NULL,
                                  `target_weight_kg` DECIMAL(5,2) NULL,
                                  `gender` VARCHAR(10) NULL,
                                  `birth_date` DATE NULL,
                                  `activity_level` VARCHAR(20) NULL,
                                  `bmr` INT NULL,
                                  `daily_kcal` INT NULL,
                                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  CONSTRAINT `fk_user_body_info_user`
                                      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE,
                                  KEY `idx_user_body_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `password_reset_token` (
                                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                        `user_id` BIGINT UNSIGNED NOT NULL,
                                        `token` VARCHAR(100) NOT NULL,
                                        `expires_at` DATETIME NOT NULL,
                                        `used` TINYINT(1) NOT NULL DEFAULT 0,
                                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_password_reset_token_token` (`token`),
                                        KEY `idx_password_reset_token_user_used` (`user_id`, `used`),
                                        CONSTRAINT `fk_password_reset_token_user`
                                            FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
                                                ON DELETE CASCADE
                                                ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `user_refresh_token` (
                                      `user_id` BIGINT UNSIGNED NOT NULL,
                                      `refresh_token` VARCHAR(512) NOT NULL,
                                      `expires_at` DATETIME NOT NULL,
                                      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`user_id`),
                                      CONSTRAINT `fk_urt_user`
                                          FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


/* =========================================================
   2-3. MEAL MASTER
========================================================= */

CREATE TABLE `meal` (
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        `food_cd` VARCHAR(50) NOT NULL,
                        `food_nm_kr` VARCHAR(100) NOT NULL,
                        `db_grp_cm` VARCHAR(5) NOT NULL,
                        `db_grp_nm` VARCHAR(20) NOT NULL,
                        `food_cat1_cd` VARCHAR(10) NULL,
                        `food_cat1_nm` VARCHAR(50) NULL,
                        `food_cat2_cd` VARCHAR(10) NULL,
                        `food_cat2_nm` VARCHAR(50) NULL,
                        `food_cat3_cd` VARCHAR(10) NULL,
                        `food_cat3_nm` VARCHAR(50) NULL,
                        `serving_size_raw` VARCHAR(50) NOT NULL,
                        `source_type` VARCHAR(20) NOT NULL DEFAULT 'PUBLIC_API',
                        `category` VARCHAR(50) NULL,
                        `brand` VARCHAR(100) NULL,
                        `tags` VARCHAR(255) NULL,
                        `update_date` DATE NULL,
                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_meal_food_cd` (`food_cd`),
                        KEY `idx_meal_cat1` (`food_cat1_cd`),
                        KEY `idx_meal_cat2` (`food_cat2_cd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `meal_nutrition` (
                                  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  `meal_id` BIGINT UNSIGNED NOT NULL,
                                  `base_amount_g` DECIMAL(8,3) NOT NULL,
                                  `kcal` DECIMAL(8,3) DEFAULT NULL,
                                  `carbohydrate` DECIMAL(8,3) DEFAULT NULL,
                                  `protein` DECIMAL(8,3) DEFAULT NULL,
                                  `fat` DECIMAL(8,3) DEFAULT NULL,
                                  `sugar` DECIMAL(8,3) DEFAULT NULL,
                                  `sodium` DECIMAL(10,3) DEFAULT NULL,
                                  `dietary_fiber` DECIMAL(8,3) DEFAULT NULL,
                                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  KEY `idx_meal_nutrition_meal_id` (`meal_id`),
                                  CONSTRAINT `fk_meal_nutrition_meal`
                                      FOREIGN KEY (`meal_id`) REFERENCES `meal`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


/* =========================================================
   2-4. DIET (user 참조), DIET 하위 (diet_score, diet_info)
========================================================= */

CREATE TABLE `diet` (
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        `user_id` BIGINT UNSIGNED NOT NULL,
                        `date` DATETIME NOT NULL,
                        `meal_type` CHAR(1) NOT NULL,
                        `description` VARCHAR(255),
                        `image_url` VARCHAR(255),
                        `source_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
                        `total_kcal` DECIMAL(8,3) DEFAULT NULL,
                        `total_carbohydrate` DECIMAL(8,3) DEFAULT NULL,
                        `total_protein` DECIMAL(8,3) DEFAULT NULL,
                        `total_fat` DECIMAL(8,3) DEFAULT NULL,
                        `total_sugar` DECIMAL(8,3) DEFAULT NULL,
                        `total_sodium` DECIMAL(10,3) DEFAULT NULL,
                        `is_public` TINYINT(1) NOT NULL DEFAULT 0,
                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        `deleted_at` DATETIME NULL,
                        PRIMARY KEY (`id`),
                        KEY `idx_diet_user_date` (`user_id`, `date`),
                        KEY `idx_diet_user_meal` (`user_id`, `date`, `meal_type`),
                        CONSTRAINT `fk_diet_user_id`
                            FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `diet_score` (
                              `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                              `diet_id` BIGINT UNSIGNED NOT NULL,
                              `score` INT NOT NULL DEFAULT 0,
                              `grade` CHAR(1) DEFAULT NULL,
                              `feedback` VARCHAR(255) DEFAULT NULL,
                              `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_diet_score_diet_id` (`diet_id`),
                              CONSTRAINT `fk_diet_score_diet_id`
                                  FOREIGN KEY (`diet_id`) REFERENCES `diet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `user_daily_score` (
                                    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                    `user_id` BIGINT UNSIGNED NOT NULL,
                                    `date` DATE NOT NULL,
                                    `score` INT NOT NULL DEFAULT 0,
                                    `grade` CHAR(1) DEFAULT NULL,
                                    `feedback` VARCHAR(255) DEFAULT NULL,
                                    `daily_kcal` INT DEFAULT NULL,
                                    `total_kcal` DECIMAL(8,3) DEFAULT 0,
                                    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    UNIQUE KEY `uk_user_date` (`user_id`, `date`),
                                    KEY `idx_user_date` (`user_id`, `date`),
                                    CONSTRAINT `fk_user_daily_score_user`
                                        FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `diet_info` (
                             `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                             `diet_id` BIGINT UNSIGNED NOT NULL,
                             `meal_id` BIGINT UNSIGNED NULL,
                             `food_nm_kr` VARCHAR(100) NOT NULL,
                             `food_code` VARCHAR(50) DEFAULT NULL,
                             `source_type` VARCHAR(20) NOT NULL DEFAULT 'PUBLIC_API',
                             `intake_gram` DECIMAL(8,3) DEFAULT NULL,
                             `kcal` DECIMAL(8,3) DEFAULT NULL,
                             `carbohydrate` DECIMAL(8,3) DEFAULT NULL,
                             `protein` DECIMAL(8,3) DEFAULT NULL,
                             `fat` DECIMAL(8,3) DEFAULT NULL,
                             `sugar` DECIMAL(8,3) DEFAULT NULL,
                             `sodium` DECIMAL(10,3) DEFAULT NULL,
                             `dietary_fiber` DECIMAL(8,3) DEFAULT NULL,
                             `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             PRIMARY KEY (`id`),
                             KEY `idx_diet_info_diet_id` (`diet_id`),
                             KEY `idx_diet_info_meal_id` (`meal_id`),
                             CONSTRAINT `fk_diet_info_diet_id`
                                 FOREIGN KEY (`diet_id`) REFERENCES `diet`(`id`) ON DELETE CASCADE,
                             CONSTRAINT `fk_diet_info_meal_id`
                                 FOREIGN KEY (`meal_id`) REFERENCES `meal`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


/* =========================================================
   2-5. TRAINING VIDEO
========================================================= */

CREATE TABLE `training_video` (
                                  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                  `youtube_video_id` VARCHAR(50) NOT NULL,
                                  `title` VARCHAR(200) NOT NULL,
                                  `description` TEXT NULL,
                                  `thumbnail_url` VARCHAR(255) NOT NULL,
                                  `duration_sec` INT NOT NULL,
                                  `main_category` VARCHAR(50) NOT NULL,
                                  `level` VARCHAR(20) NULL,
                                  `tags` VARCHAR(255) NULL,
                                  `view_count` BIGINT NOT NULL DEFAULT 0,
                                  `like_count` BIGINT NOT NULL DEFAULT 0,
                                  `youtube_view_cnt` BIGINT NULL,
                                  `youtube_like_cnt` BIGINT NULL,
                                  `published_at` DATETIME NULL,
                                  `is_recommended` TINYINT(1) NOT NULL DEFAULT 0,
                                  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
                                  `channel_title` VARCHAR(200) NULL,
                                  `created_at` DATETIME NOT NULL,
                                  `updated_at` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `training_video_like` (
                                       `user_id` BIGINT UNSIGNED NOT NULL,
                                       `video_id` BIGINT UNSIGNED NOT NULL,
                                       `created_at` DATETIME NOT NULL,
                                       PRIMARY KEY (`user_id`, `video_id`),
                                       CONSTRAINT `fk_training_video_like_user`
                                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
                                       CONSTRAINT `fk_training_video_like_video`
                                           FOREIGN KEY (`video_id`) REFERENCES `training_video`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `training_youtube_source` (
                                           `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                           `source_type` VARCHAR(20) NOT NULL,
                                           `source_value` VARCHAR(255) NOT NULL,
                                           `main_category` VARCHAR(50) NOT NULL,
                                           `level` VARCHAR(20) NULL,
                                           `max_results` INT NOT NULL DEFAULT 20,
                                           `is_active` TINYINT(1) NOT NULL DEFAULT 1,
                                           `created_at` DATETIME NOT NULL,
                                           `updated_at` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO training_youtube_source(source_type, source_value, main_category, level, max_results, is_active, created_at, updated_at)
VALUES
    ('KEYWORD', '전신운동 15분', 'HOME', 'BEGINNER', 20, 1, NOW(), NOW()),
    ('KEYWORD', '요가 스트레칭 10분', 'YOGA', 'BEGINNER', 20, 1, NOW(), NOW()),
    ('KEYWORD', '필라테스 core workout', 'PILATES', 'ALL', 20, 1, NOW(), NOW());

CREATE TABLE `training_video_history` (
                                          `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                          `user_id` BIGINT UNSIGNED NOT NULL,
                                          `video_id` BIGINT UNSIGNED NOT NULL,
                                          `activity_date` DATE NOT NULL,
                                          `completed` TINYINT(1) NOT NULL DEFAULT 1,
                                          `duration_sec` INT NULL,
                                          `memo` VARCHAR(255) NULL,
                                          `created_at` DATETIME NOT NULL,
                                          `updated_at` DATETIME NOT NULL,
                                          CONSTRAINT `fk_tv_history_user`
                                              FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                          CONSTRAINT `fk_tv_history_video`
                                              FOREIGN KEY (`video_id`) REFERENCES `training_video`(`id`) ON DELETE CASCADE,
                                          CONSTRAINT `uq_tv_history_user_video_date`
                                              UNIQUE (`user_id`, `video_id`, `activity_date`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `training_video_comment` (
                                          `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                          `video_id` BIGINT UNSIGNED NOT NULL,
                                          `user_id` BIGINT UNSIGNED NOT NULL,
                                          `content` VARCHAR(500) NOT NULL,
                                          `created_at` DATETIME NOT NULL,
                                          `updated_at` DATETIME NOT NULL,
                                          `deleted_at` DATETIME NULL,
                                          CONSTRAINT `fk_tvc_video`
                                              FOREIGN KEY (`video_id`) REFERENCES `training_video`(`id`) ON DELETE CASCADE,
                                          CONSTRAINT `fk_tvc_user`
                                              FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


/* =========================================================
   2-6. COMMUNITY
========================================================= */

CREATE TABLE `community_post` (
                                  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                  `user_id` BIGINT UNSIGNED NOT NULL,
                                  `title` VARCHAR(200) NOT NULL,
                                  `content` TEXT NOT NULL,
                                  `category` VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
                                  `post_image_url` VARCHAR(255) NULL,
                                  `activity_date` DATE NULL,
                                  `share_diet` TINYINT(1) NOT NULL DEFAULT 0,
                                  `share_streak` TINYINT(1) NOT NULL DEFAULT 0,
                                  `share_workout` TINYINT(1) NOT NULL DEFAULT 0,
                                  `diet_note` VARCHAR(255) NULL,
                                  `workout_note` VARCHAR(255) NULL,
                                  `like_count` INT UNSIGNED NOT NULL DEFAULT 0,
                                  `view_count` INT UNSIGNED NOT NULL DEFAULT 0,
                                  `comment_count` INT UNSIGNED NOT NULL DEFAULT 0,
                                  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
                                  `deleted_at` DATETIME NULL,
                                  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
                                  INDEX `idx_post_user` (`user_id`),
                                  INDEX `idx_post_category` (`category`),
                                  INDEX `idx_post_deleted` (`is_deleted`, `deleted_at`),
                                  INDEX `idx_post_popular` (`like_count`, `view_count`, `created_at` DESC),
                                  INDEX `idx_post_activity` (`activity_date`),
                                  CONSTRAINT `fk_community_post_user`
                                      FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `community_post_diet` (
                                       `post_id` BIGINT UNSIGNED NOT NULL,
                                       `diet_id` BIGINT UNSIGNED NOT NULL,
                                       PRIMARY KEY (`post_id`, `diet_id`),
                                       INDEX `idx_cpd_diet` (`diet_id`),
                                       CONSTRAINT `fk_cpd_post`
                                           FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
                                       CONSTRAINT `fk_cpd_diet`
                                           FOREIGN KEY (`diet_id`) REFERENCES `diet`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `community_post_video` (
                                        `post_id` BIGINT UNSIGNED NOT NULL,
                                        `video_id` BIGINT UNSIGNED NOT NULL,
                                        PRIMARY KEY (`post_id`, `video_id`),
                                        INDEX `idx_cpv_video` (`video_id`),
                                        CONSTRAINT `fk_cpv_post`
                                            FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
                                        CONSTRAINT `fk_cpv_video`
                                            FOREIGN KEY (`video_id`) REFERENCES `training_video`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `community_tag` (
                                 `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                 `name` VARCHAR(50) NOT NULL,
                                 `usage_count` INT UNSIGNED NOT NULL DEFAULT 0,
                                 `created_at` DATETIME NOT NULL,
                                 `updated_at` DATETIME NOT NULL,
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `community_post_tag` (
                                      `post_id` BIGINT UNSIGNED NOT NULL,
                                      `tag_id`  BIGINT UNSIGNED NOT NULL,
                                      PRIMARY KEY (`post_id`, `tag_id`),
                                      INDEX `idx_posttag_tag` (`tag_id`),
                                      CONSTRAINT `fk_posttag_post`
                                          FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
                                      CONSTRAINT `fk_posttag_tag`
                                          FOREIGN KEY (`tag_id`) REFERENCES `community_tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `community_post_like` (
                                       `post_id` BIGINT UNSIGNED NOT NULL,
                                       `user_id` BIGINT UNSIGNED NOT NULL,
                                       `created_at` DATETIME NOT NULL,
                                       PRIMARY KEY (`post_id`, `user_id`),
                                       INDEX `idx_like_user` (`user_id`, `created_at` DESC),
                                       CONSTRAINT `fk_like_post`
                                           FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
                                       CONSTRAINT `fk_like_user`
                                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `community_comment` (
                                     `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     `post_id` BIGINT UNSIGNED NOT NULL,
                                     `user_id` BIGINT UNSIGNED NOT NULL,
                                     `content` VARCHAR(500) NOT NULL,
                                     `parent_id` BIGINT UNSIGNED NULL,
                                     `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
                                     `deleted_at` DATETIME NULL,
                                     `created_at` DATETIME NOT NULL,
                                     `updated_at` DATETIME NOT NULL,
                                     PRIMARY KEY (`id`),
                                     INDEX `idx_comment_post` (`post_id`),
                                     INDEX `idx_comment_user` (`user_id`),
                                     CONSTRAINT `fk_comment_post`
                                         FOREIGN KEY (`post_id`) REFERENCES `community_post`(`id`) ON DELETE CASCADE,
                                     CONSTRAINT `fk_comment_user`
                                         FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;


/* =========================================================
   2-7. DASHBOARD / AI LOG
========================================================= */

CREATE TABLE `user_daily_nutrition` (
                                        `user_id` BIGINT UNSIGNED NOT NULL,
                                        `date` DATE NOT NULL,
                                        `total_kcal` DECIMAL(10,3) NOT NULL DEFAULT 0,
                                        `total_carbohydrate` DECIMAL(10,3) NOT NULL DEFAULT 0,
                                        `total_protein` DECIMAL(10,3) NOT NULL DEFAULT 0,
                                        `total_fat` DECIMAL(10,3) NOT NULL DEFAULT 0,
                                        `total_sugar` DECIMAL(10,3) NOT NULL DEFAULT 0,
                                        `total_sodium` DECIMAL(12,3) NOT NULL DEFAULT 0,
                                        `diet_score_avg` INT NULL,
                                        `diet_score_sum` INT NULL,
                                        `meal_count` INT NOT NULL DEFAULT 0,
                                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`user_id`, `date`),
                                        CONSTRAINT `fk_udn_user`
                                            FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `user_daily_activity` (
                                       `user_id` BIGINT UNSIGNED NOT NULL,
                                       `date` DATE NOT NULL,
                                       `diet_logged` TINYINT(1) NOT NULL DEFAULT 0,
                                       `workout_logged` TINYINT(1) NOT NULL DEFAULT 0,
                                       `challenge_done` TINYINT(1) NOT NULL DEFAULT 0,
                                       `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       PRIMARY KEY (`user_id`, `date`),
                                       CONSTRAINT `fk_uda_user`
                                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `diet_ai_weekly_analysis_log` (
                                               `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                               `user_id` BIGINT UNSIGNED NOT NULL,
                                               `start_date` DATE NOT NULL,
                                               `end_date` DATE NOT NULL,
                                               `request_system_prompt` TEXT NOT NULL,
                                               `request_user_prompt` TEXT NOT NULL,
                                               `response_json` JSON NULL,
                                               `weekly_score` INT NULL,
                                               `status` VARCHAR(20) NOT NULL,
                                               `error_message` VARCHAR(1000) NULL,
                                               `created_at` DATETIME NOT NULL,
                                               `updated_at` DATETIME NOT NULL,
                                               INDEX `idx_user_period` (`user_id`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


/* =========================================================
   2-8. SEED HISTORY
========================================================= */

CREATE TABLE IF NOT EXISTS seed_history (
                                            `name` VARCHAR(50) NOT NULL,
                                            `executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
