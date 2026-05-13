ALTER TABLE `airag_app`
ADD COLUMN `voice_model_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Voice model id' AFTER `model_id`;
