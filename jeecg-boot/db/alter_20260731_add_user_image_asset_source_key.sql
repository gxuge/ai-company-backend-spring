ALTER TABLE `ts_user_image_asset`
    ADD COLUMN `source_key` VARCHAR(64) NULL COMMENT '来源唯一标识，例如Agent Event ID' AFTER `source_type`,
    ADD UNIQUE KEY `uk_user_image_asset_source` (`user_id`, `source_key`);
