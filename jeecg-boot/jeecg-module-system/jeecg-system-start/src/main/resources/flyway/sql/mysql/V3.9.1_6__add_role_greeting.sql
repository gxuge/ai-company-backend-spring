-- Add greeting field to ts_role_info
ALTER TABLE ts_role_info
    ADD COLUMN greeting TEXT NULL COMMENT '角色开场白' AFTER intro_text;