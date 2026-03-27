SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================================
-- ai-company.sql
-- 说明：已按依赖关系整理排序，并为每张表补充中文用途说明
-- ====================================================

-- -------------------------------------
-- 角色主表：维护角色基础信息与展示配置
-- 表名：ts_role_info
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_info`;
CREATE TABLE ts_role_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '作者用户ID，对应 sys_user.id',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    role_subtitle VARCHAR(200) DEFAULT NULL COMMENT '角色副标题/补充身份文案',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '角色头像URL',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '角色封面图URL',
    gender VARCHAR(20) DEFAULT NULL COMMENT '角色性别，如：male/female/random',
    occupation VARCHAR(100) DEFAULT NULL COMMENT '角色职业',
    intro_text VARCHAR(1000) DEFAULT NULL COMMENT '角色简介文本',
    persona_text TEXT COMMENT '角色人设/设定描述',
    background_story TEXT COMMENT '角色背景故事/人物设定',
    story_text LONGTEXT COMMENT '故事Tab内容，当前先按单文本保存',
    dialogue_preview TEXT COMMENT '对话风格预览文案',
    dialogue_length VARCHAR(20) DEFAULT NULL COMMENT '对话长度，如：short/detailed',
    tone_tendency VARCHAR(100) DEFAULT NULL COMMENT '语气倾向，如：幽默、温柔、傲娇',
    interaction_mode VARCHAR(20) DEFAULT NULL COMMENT '互动模式，如：active/passive',
    voice_name VARCHAR(100) DEFAULT NULL COMMENT '角色声音名称',
    ext_json JSON COMMENT '角色扩展配置JSON，预留模型参数、展示配置等',
    is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开：1=公开，0=不公开',
    basic_ai_generated TINYINT(1) NOT NULL DEFAULT 0 COMMENT '基础信息是否由AI一键生成：1=是，0=否',
    advanced_ai_generated TINYINT(1) NOT NULL DEFAULT 0 COMMENT '高级设置是否由AI推荐/生成：1=是，0=否',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：1=正常，0=停用/删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_role_name (role_name),
    KEY idx_status (status),
    CONSTRAINT fk_role_info_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色主表';

-- -------------------------------------
-- 用户角色标签表：维护用户自定义角色标签
-- 表名：ts_user_role_tag
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_role_tag`;
CREATE TABLE ts_user_role_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标签ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称，如：傲娇、极客、高冷',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '标签状态：1=启用，0=停用/删除',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tag_name (user_id, tag_name),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_user_role_tag_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色标签表';

-- -------------------------------------
-- 用户图片资源表：保存用户上传或引用的图片素材
-- 表名：ts_user_image_asset
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_image_asset`;
CREATE TABLE ts_user_image_asset (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '图片素材ID，主键',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '所属用户ID，仅记录，不做外键强绑定',
    file_url VARCHAR(500) NOT NULL COMMENT '原图URL',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名称',
    mime_type VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型，如 image/png',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小，单位字节',
    width INT DEFAULT NULL COMMENT '图片宽度，单位像素',
    height INT DEFAULT NULL COMMENT '图片高度，单位像素',
    source_type VARCHAR(20) NOT NULL COMMENT '素材来源：upload=用户上传，generate=AI生成',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '素材状态：1=正常，0=软删除/停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_source_type (source_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户图片素材表';

-- -------------------------------------
-- 角色创建历史表：记录角色创建与保存版本
-- 表名：ts_role_create_history
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_create_history`;
CREATE TABLE ts_role_create_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '历史记录ID，主键',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
    version_no INT NOT NULL DEFAULT 1 COMMENT '版本号，同一角色每保存一次递增',
    operation_type VARCHAR(20) NOT NULL DEFAULT 'create' COMMENT '操作类型：create=创建，save=保存，update=更新',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',

    role_name VARCHAR(100) NOT NULL COMMENT '当时的角色名称',
    gender VARCHAR(20) DEFAULT NULL COMMENT '当时的角色性别，如：male/female/random',
    occupation VARCHAR(100) DEFAULT NULL COMMENT '当时的角色职业',
    background_story TEXT COMMENT '当时的角色背景故事/人物设定',
    voice_name VARCHAR(100) DEFAULT NULL COMMENT '当时选择的声音名称',
    is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当时是否公开：1=公开，0=不公开',

    dialogue_preview TEXT COMMENT '当时的对话风格预览文案',
    dialogue_length VARCHAR(20) DEFAULT NULL COMMENT '当时的对话长度，如：short/detailed',
    tone_tendency VARCHAR(100) DEFAULT NULL COMMENT '当时的语气倾向',
    interaction_mode VARCHAR(20) DEFAULT NULL COMMENT '当时的互动模式，如：active/passive',

    basic_ai_generated TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当次基础信息是否由AI生成：1=是，0=否',
    advanced_ai_generated TINYINT(1) NOT NULL DEFAULT 0 COMMENT '当次高级设置是否由AI生成：1=是，0=否',

    tags_json JSON DEFAULT NULL COMMENT '当时选中的标签名称快照，JSON数组，如：[\"傲娇\",\"极客\"]',
    snapshot_json JSON DEFAULT NULL COMMENT '当次整份角色配置快照，JSON对象',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '历史记录创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '历史记录更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_role_version (role_id, version_no),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    KEY idx_created_at (created_at),
    KEY idx_operation_type (operation_type),
    CONSTRAINT fk_role_create_history_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色创建历史表';

-- -------------------------------------
-- 角色图片参考表：存放角色图片生成参考素材
-- 表名：ts_role_image_reference
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_image_reference`;
CREATE TABLE ts_role_image_reference (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参考图关联ID，主键',
    image_profile_id BIGINT UNSIGNED NOT NULL COMMENT '角色形象配置ID，仅记录，不做外键强绑定',
    asset_id BIGINT UNSIGNED DEFAULT NULL COMMENT '参考图片素材ID，仅记录，不做外键强绑定',
    asset_url VARCHAR(500) DEFAULT NULL COMMENT '参考图片URL快照，避免素材清理影响展示',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_profile_asset (image_profile_id, asset_id),
    KEY idx_image_profile_id (image_profile_id),
    KEY idx_asset_id (asset_id),
    KEY idx_profile_sort (image_profile_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色形象参考图关联表';

-- -------------------------------------
-- 角色形象配置表：保存角色当前形象设置
-- 表名：ts_role_image_profile
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_image_profile`;
CREATE TABLE ts_role_image_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色形象配置ID，主键',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
    prompt_text TEXT COMMENT '形象描述文本，如性别、外貌、性格、衣着等',
    style_name VARCHAR(100) DEFAULT NULL COMMENT '风格名称，如 通用、像素艺术、漫画、厚涂',

    selected_image_asset_id BIGINT UNSIGNED DEFAULT NULL COMMENT '当前最终采用的图片素材ID，仅作记录，不做外键强绑定',
    selected_image_url VARCHAR(500) DEFAULT NULL COMMENT '当前最终采用图片URL快照，避免素材清理影响回显',

    source_type VARCHAR(20) NOT NULL DEFAULT 'ai_generate' COMMENT '形象来源：ai_generate=AI生成，manual_upload=手动上传',
    ext_json JSON COMMENT '扩展配置JSON，如生成参数、模型参数、尺寸参数等',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_role_id (role_id),
    KEY idx_style_name (style_name),
    KEY idx_selected_image_asset_id (selected_image_asset_id),
    CONSTRAINT fk_role_image_profile_role
        FOREIGN KEY (role_id) REFERENCES ts_role_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色形象配置表';

-- -------------------------------------
-- 角色图片生成记录表：记录图片生成任务与结果
-- 表名：ts_role_image_generate_record
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_image_generate_record`;
CREATE TABLE ts_role_image_generate_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '生成记录ID，主键',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
    image_profile_id BIGINT UNSIGNED DEFAULT NULL COMMENT '角色形象配置ID，对应 ts_role_image_profile.id',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '发起生成的用户ID，对应 ts_user_info.id',

    prompt_text TEXT COMMENT '本次生成使用的形象描述文本',
    style_name VARCHAR(100) DEFAULT NULL COMMENT '本次生成使用的风格名称',
    reference_assets_json JSON COMMENT '本次生成使用的参考图信息JSON，如素材ID列表或URL列表',
    generate_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '生成状态：pending=待处理，success=成功，failed=失败',

    result_asset_id BIGINT UNSIGNED DEFAULT NULL COMMENT '生成成功后的结果图片素材ID，仅作记录，不做外键强绑定',
    result_image_url VARCHAR(500) DEFAULT NULL COMMENT '生成结果图片URL快照，避免素材清理影响历史回溯',

    fail_reason VARCHAR(500) DEFAULT NULL COMMENT '生成失败原因',
    request_id VARCHAR(100) DEFAULT NULL COMMENT '请求流水号/任务ID，便于和AI服务侧对账',
    ext_json JSON COMMENT '扩展参数JSON，如模型名、采样参数、尺寸、种子等',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_role_id (role_id),
    KEY idx_image_profile_id (image_profile_id),
    KEY idx_user_id (user_id),
    KEY idx_generate_status (generate_status),
    KEY idx_result_asset_id (result_asset_id),
    KEY idx_request_id (request_id),

    CONSTRAINT fk_role_image_generate_record_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色形象生成记录表';

-- -------------------------------------
-- 角色关于信息表：保存角色主页附加信息项
-- 表名：ts_role_about_item
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_about_item`;
CREATE TABLE ts_role_about_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关于TA条目ID，主键',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',

    item_type VARCHAR(50) DEFAULT NULL COMMENT '条目类型，如 profile/trait/habit/tab/custom',
    item_title VARCHAR(100) DEFAULT NULL COMMENT '条目标题，如 身份、性格、喜好、补充设定',
    item_subtitle VARCHAR(200) DEFAULT NULL COMMENT '条目副标题/补充说明',
    item_json JSON COMMENT '条目内容JSON，结构不固定，由前端和业务自行约定',
    item_text LONGTEXT COMMENT '条目纯文本内容，便于兜底展示或全文检索',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '条目状态：1=正常，0=停用/删除',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_role_id (role_id),
    KEY idx_role_sort (role_id, sort_no),
    KEY idx_item_type (item_type),
    KEY idx_status (status),
    CONSTRAINT fk_role_about_item_role
        FOREIGN KEY (role_id) REFERENCES ts_role_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色关于TA条目表';

-- -------------------------------------
-- 角色统计表：保存角色互动与使用统计
-- 表名：ts_role_stat
-- -------------------------------------
DROP TABLE IF EXISTS `ts_role_stat`;
CREATE TABLE ts_role_stat (
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
    connector_count BIGINT NOT NULL DEFAULT 0 COMMENT '连接者数量',
    follower_count BIGINT NOT NULL DEFAULT 0 COMMENT '粉丝数量',
    dialogue_count BIGINT NOT NULL DEFAULT 0 COMMENT '对话数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '统计更新时间',
    PRIMARY KEY (role_id),
    CONSTRAINT fk_role_stat_role
        FOREIGN KEY (role_id) REFERENCES ts_role_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色统计表';

-- -------------------------------------
-- 故事主表：保存故事基础信息与创作配置
-- 表名：ts_story_info
-- -------------------------------------
DROP TABLE IF EXISTS `ts_story_info`;
CREATE TABLE `ts_story_info` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `story_code` VARCHAR(64) NOT NULL COMMENT '故事编号',
  `user_id` VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '作者用户ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '故事标题',
  `story_intro` VARCHAR(1000) DEFAULT NULL COMMENT '故事简介',
  `story_mode` VARCHAR(20) NOT NULL DEFAULT 'chapter' COMMENT '剧情模式：normal普通，chapter章节',
  `story_setting` TEXT COMMENT '故事整体设定/世界观',
  `story_background` TEXT COMMENT '故事背景补充说明',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '故事封面URL',
  `scene_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '场景/门店ID',
  `scene_name_snapshot` VARCHAR(255) DEFAULT NULL COMMENT '场景名称快照',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1已提交，2已发布，9删除',
  `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '是否公开：1是，0否',
  `is_ai_story_setting` TINYINT NOT NULL DEFAULT 0 COMMENT '故事设定是否AI生成',
  `is_ai_character` TINYINT NOT NULL DEFAULT 0 COMMENT '角色是否AI生成',
  `is_ai_outline` TINYINT NOT NULL DEFAULT 0 COMMENT '大纲是否AI生成',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by` VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '创建人ID',
  `created_name` VARCHAR(100) DEFAULT NULL COMMENT '创建人名称',
  `updated_by` VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '更新人ID',
  `updated_name` VARCHAR(100) DEFAULT NULL COMMENT '更新人名称',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_story_code` (`story_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_updated_by` (`updated_by`),
  KEY `idx_scene_id` (`scene_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_story_info_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_story_info_created_by`
    FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_story_info_updated_by`
    FOREIGN KEY (`updated_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事主表';

-- -------------------------------------
-- 故事角色关系表：保存故事与角色的关联关系
-- 表名：ts_story_role_rel
-- -------------------------------------
DROP TABLE IF EXISTS `ts_story_role_rel`;
CREATE TABLE ts_story_role_rel (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    story_id BIGINT UNSIGNED NOT NULL COMMENT '故事ID，对应 ts_story_info.id',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
    role_type VARCHAR(30) DEFAULT 'support' COMMENT '角色定位: user/main/support/npc',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    is_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否必选',
    join_source VARCHAR(20) DEFAULT 'manual' COMMENT '加入来源: manual/ai',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_story_role (story_id, role_id),
    KEY idx_story_id (story_id),
    KEY idx_role_id (role_id),
    CONSTRAINT fk_story_role_rel_story
        FOREIGN KEY (story_id) REFERENCES ts_story_info (id),
    CONSTRAINT fk_story_role_rel_role
        FOREIGN KEY (role_id) REFERENCES ts_role_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事角色关联表';

-- -------------------------------------
-- 故事统计表：保存故事浏览与互动统计
-- 表名：ts_story_stat
-- -------------------------------------
DROP TABLE IF EXISTS `ts_story_stat`;
CREATE TABLE ts_story_stat (
    story_id BIGINT UNSIGNED NOT NULL COMMENT '故事ID，对应 ts_story_info.id',
    follower_count BIGINT NOT NULL DEFAULT 0 COMMENT '粉丝数量',
    dialogue_count BIGINT NOT NULL DEFAULT 0 COMMENT '对话数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '统计更新时间',
    PRIMARY KEY (story_id),
    CONSTRAINT fk_story_stat_story
        FOREIGN KEY (story_id) REFERENCES ts_story_info (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事统计表';

-- -------------------------------------
-- 故事章节表：保存章节结构与开场信息
-- 表名：ts_story_chapter
-- -------------------------------------
DROP TABLE IF EXISTS `ts_story_chapter`;
CREATE TABLE `ts_story_chapter` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '章节ID',
  `story_id` BIGINT UNSIGNED NOT NULL COMMENT '故事ID，对应 ts_story_info.id',
  `chapter_no` INT NOT NULL COMMENT '章节序号',
  `chapter_title` VARCHAR(100) DEFAULT NULL COMMENT '章节标题',
  `chapter_desc` TEXT COMMENT '章节描述/主要情节',
  `opening_content` TEXT COMMENT '开场白内容',
  `opening_role_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '开场白角色ID，对应 ts_role_info.id',
  `mission_target` TEXT COMMENT '任务目标',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0删除 1有效',
  `is_ai_generated` TINYINT NOT NULL DEFAULT 0 COMMENT '本章是否AI生成',
  `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_story_chapter_no` (`story_id`, `chapter_no`),
  KEY `idx_story_id` (`story_id`),
  KEY `idx_opening_role_id` (`opening_role_id`),
  CONSTRAINT `fk_story_chapter_story`
    FOREIGN KEY (`story_id`) REFERENCES `ts_story_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_story_chapter_opening_role`
    FOREIGN KEY (`opening_role_id`) REFERENCES `ts_role_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事章节表';

-- -------------------------------------
-- 章节禁用角色表：保存章节中禁止出场的角色
-- 表名：ts_story_chapter_forbidden_role
-- -------------------------------------
DROP TABLE IF EXISTS `ts_story_chapter_forbidden_role`;
CREATE TABLE `ts_story_chapter_forbidden_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `story_id` BIGINT UNSIGNED NOT NULL COMMENT '故事ID，对应 ts_story_info.id',
  `chapter_id` BIGINT UNSIGNED NOT NULL COMMENT '章节ID，对应 ts_story_chapter.id',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID，对应 ts_role_info.id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chapter_forbidden_role` (`chapter_id`, `role_id`),
  KEY `idx_story_id` (`story_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_forbidden_story`
    FOREIGN KEY (`story_id`) REFERENCES `ts_story_info` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_forbidden_chapter`
    FOREIGN KEY (`chapter_id`) REFERENCES `ts_story_chapter` (`id`),
  CONSTRAINT `fk_forbidden_role`
    FOREIGN KEY (`role_id`) REFERENCES `ts_role_info` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节禁止出场角色表（关联 ts_role_info）';

-- -------------------------------------
-- 会话主表：保存用户会话与目标对象信息
-- 表名：ts_chat_session
-- -------------------------------------
DROP TABLE IF EXISTS `ts_chat_session`;
CREATE TABLE ts_chat_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',
    session_type VARCHAR(20) NOT NULL COMMENT '会话类型：single=单角色对话，story=故事多人对话',
    session_title VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    target_role_id BIGINT UNSIGNED DEFAULT NULL COMMENT '目标角色ID，单聊时使用，对应 ts_role_info.id',
    story_id BIGINT UNSIGNED DEFAULT NULL COMMENT '故事ID，故事会话时使用，对应 ts_story_info.id',
    session_status TINYINT NOT NULL DEFAULT 1 COMMENT '会话状态：1=正常，0=关闭/删除',
    last_message_id BIGINT UNSIGNED DEFAULT NULL COMMENT '最后一条消息ID，冗余字段，便于列表展示',
    last_message_at DATETIME DEFAULT NULL COMMENT '最后发言时间，冗余字段，便于列表排序',
    ext_json JSON COMMENT '会话扩展配置JSON，如上下文参数、模型参数等',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_session_type (session_type),
    KEY idx_target_role_id (target_role_id),
    KEY idx_story_id (story_id),
    KEY idx_last_message_at (last_message_at),
    CONSTRAINT fk_chat_session_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_chat_session_role
        FOREIGN KEY (target_role_id) REFERENCES ts_role_info (id),
    CONSTRAINT fk_chat_session_story
        FOREIGN KEY (story_id) REFERENCES ts_story_info (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- -------------------------------------
-- 会话成员表：保存群聊或多角色会话成员
-- 表名：ts_chat_session_member
-- -------------------------------------
DROP TABLE IF EXISTS `ts_chat_session_member`;
CREATE TABLE ts_chat_session_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID，对应 ts_chat_session.id',
    member_type VARCHAR(20) NOT NULL COMMENT '成员类型：user=用户，role=角色，system=系统，narrator=旁白',
    member_id BIGINT UNSIGNED DEFAULT NULL COMMENT '成员ID；当 member_type=user 时对应 ts_user_info.id，当 member_type=role 时对应 ts_role_info.id',
    display_name VARCHAR(100) DEFAULT NULL COMMENT '成员显示名称，冗余保存，避免频繁联表',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '成员头像URL，冗余保存',
    is_owner TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否会话拥有者：1=是，0=否',
    join_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_member (session_id, member_type, member_id),
    KEY idx_member (member_type, member_id),
    CONSTRAINT fk_chat_session_member_session
        FOREIGN KEY (session_id) REFERENCES ts_chat_session (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话成员表';

-- -------------------------------------
-- 会话消息表：保存聊天消息内容与状态
-- 表名：ts_chat_message
-- -------------------------------------
DROP TABLE IF EXISTS `ts_chat_message`;
CREATE TABLE ts_chat_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID，主键',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID，对应 ts_chat_session.id',
    sender_type VARCHAR(20) NOT NULL COMMENT '发送者类型：user=用户，role=角色，system=系统，narrator=旁白',
    sender_id BIGINT UNSIGNED DEFAULT NULL COMMENT '发送者ID；当 sender_type=user 时对应 ts_user_info.id，当 sender_type=role 时对应 ts_role_info.id',
    sender_name VARCHAR(100) DEFAULT NULL COMMENT '发送者显示名称，冗余保存',
    message_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型：text=文本，voice=语音，image=图片，system=系统，action=动作描写',
    content_text LONGTEXT COMMENT '消息文本内容',
    content_json JSON COMMENT '消息扩展内容JSON，如结构化片段、富文本、语音元数据等',
    reply_to_message_id BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的消息ID，对应 ts_chat_message.id',
    seq_no BIGINT NOT NULL COMMENT '会话内顺序号，从1开始递增',
    generate_status VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '生成状态：success=成功，generating=生成中，failed=失败',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间/创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_seq (session_id, seq_no),
    KEY idx_session_id (session_id),
    KEY idx_sender (sender_type, sender_id),
    KEY idx_reply_to_message_id (reply_to_message_id),
    KEY idx_created_at (created_at),
    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (session_id) REFERENCES ts_chat_session (id),
    CONSTRAINT fk_chat_message_reply
        FOREIGN KEY (reply_to_message_id) REFERENCES ts_chat_message (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- -------------------------------------
-- 消息附件表：保存消息关联附件
-- 表名：ts_chat_message_attachment
-- -------------------------------------
DROP TABLE IF EXISTS `ts_chat_message_attachment`;
CREATE TABLE ts_chat_message_attachment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '附件ID，主键',
    message_id BIGINT UNSIGNED NOT NULL COMMENT '消息ID，对应 ts_chat_message.id',
    file_type VARCHAR(20) NOT NULL COMMENT '附件类型：voice=语音，image=图片，file=文件',
    file_url VARCHAR(500) NOT NULL COMMENT '附件访问URL',
    file_name VARCHAR(255) DEFAULT NULL COMMENT '附件文件名',
    file_size BIGINT DEFAULT NULL COMMENT '附件大小，单位字节',
    duration_sec INT DEFAULT NULL COMMENT '语音时长，单位秒，仅语音附件时使用',
    mime_type VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_message_id (message_id),
    CONSTRAINT fk_chat_message_attachment_message
        FOREIGN KEY (message_id) REFERENCES ts_chat_message (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息附件表';

-- -------------------------------------
-- 用户关注关系表：保存用户对角色或故事的关注关系
-- 表名：ts_user_follow_target
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_follow_target`;
CREATE TABLE ts_user_follow_target (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关注关系ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '发起关注的用户ID，对应 ts_user_info.id',
    target_type VARCHAR(20) NOT NULL COMMENT '关注目标类型：role=角色，story=故事',
    target_id BIGINT UNSIGNED NOT NULL COMMENT '关注目标ID；target_type=role 时对应 ts_role_info.id，target_type=story 时对应 ts_story_info.id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    KEY idx_target (target_type, target_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_user_follow_target_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- -------------------------------------
-- 用户偏好标签表：保存用户偏好设置
-- 表名：ts_user_preference_tag
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_preference_tag`;
CREATE TABLE ts_user_preference_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '偏好标签ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',
    tag_name VARCHAR(50) NOT NULL COMMENT '偏好标签名称，如 都市、职场、情感陪伴',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，数字越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '标签状态：1=正常，0=删除/停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_tag_name (user_id, tag_name),
    KEY idx_user_sort (user_id, sort_no),
    KEY idx_status (status),
    CONSTRAINT fk_user_preference_tag_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户偏好标签表';

-- -------------------------------------
-- 用户认证信息表：保存用户认证与审核状态
-- 表名：ts_user_verification
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_verification`;
CREATE TABLE ts_user_verification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '实名认证记录ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',
    verify_status VARCHAR(20) NOT NULL DEFAULT 'unverified' COMMENT '认证状态：unverified=未认证，verifying=认证中，verified=已认证，failed=认证失败',
    real_name VARCHAR(100) DEFAULT NULL COMMENT '真实姓名',
    id_card_no_masked VARCHAR(64) DEFAULT NULL COMMENT '证件号脱敏值',
    verified_at DATETIME DEFAULT NULL COMMENT '认证通过时间',
    fail_reason VARCHAR(500) DEFAULT NULL COMMENT '认证失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_verify_status (verify_status),
    CONSTRAINT fk_user_verification_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实名认证表';

-- -------------------------------------
-- 用户隐私设置表：保存用户隐私开关配置
-- 表名：ts_user_privacy_setting
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_privacy_setting`;
CREATE TABLE ts_user_privacy_setting (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '隐私设置ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属用户ID，对应 ts_user_info.id',
    show_gender TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否展示性别：1=展示，0=隐藏',
    show_birthday TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否展示生日：1=展示，0=隐藏',
    allow_recommendation TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许个性化推荐：1=允许，0=不允许',
    allow_stranger_contact TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许陌生人联系：1=允许，0=不允许',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_user_privacy_setting_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐私设置表';

-- -------------------------------------
-- 用户反馈表：保存用户意见反馈记录
-- 表名：ts_user_feedback
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_feedback`;
CREATE TABLE ts_user_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '反馈记录ID，主键',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '提交反馈的用户ID，对应 ts_user_info.id',
    feedback_type VARCHAR(30) NOT NULL DEFAULT 'suggestion' COMMENT '反馈类型：suggestion=建议，bug=问题反馈，complaint=投诉，other=其他',
    content TEXT NOT NULL COMMENT '反馈内容',
    contact_info VARCHAR(100) DEFAULT NULL COMMENT '联系方式，如手机号、邮箱、微信等',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending=待处理，processing=处理中，resolved=已解决，closed=已关闭',
    reply_content TEXT COMMENT '处理回复内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_feedback_type (feedback_type),
    KEY idx_status (status),
    CONSTRAINT fk_user_feedback_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='意见反馈表';

-- -------------------------------------
-- 音色主表：保存可选音色档案
-- 表名：ts_voice_profile
-- -------------------------------------
DROP TABLE IF EXISTS `ts_voice_profile`;
CREATE TABLE ts_voice_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '音色名称',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    gender ENUM('unknown', 'male', 'female') NOT NULL DEFAULT 'unknown' COMMENT '性别',
    age_group ENUM('child', 'teen', 'young', 'adult', 'middle', 'senior') NOT NULL DEFAULT 'adult' COMMENT '年龄段',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_gender_age_status (gender, age_group, status),
    KEY idx_sort_no (sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音色主表';

-- -------------------------------------
-- 音色标签表：维护音色标签
-- 表名：ts_voice_tag
-- -------------------------------------
DROP TABLE IF EXISTS `ts_voice_tag`;
CREATE TABLE ts_voice_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tag_name VARCHAR(30) NOT NULL COMMENT '标签名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音色标签表';

-- -------------------------------------
-- 音色标签关联表：维护音色与标签多对多关系
-- 表名：ts_voice_profile_tag
-- -------------------------------------
DROP TABLE IF EXISTS `ts_voice_profile_tag`;
CREATE TABLE ts_voice_profile_tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    voice_profile_id BIGINT UNSIGNED NOT NULL COMMENT '音色ID',
    tag_id BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_voice_tag (voice_profile_id, tag_id),
    KEY idx_tag_id (tag_id),
    CONSTRAINT fk_voice_profile_tag_profile
        FOREIGN KEY (voice_profile_id) REFERENCES ts_voice_profile (id),
    CONSTRAINT fk_voice_profile_tag_tag
        FOREIGN KEY (tag_id) REFERENCES ts_voice_tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音色与标签关系表';

-- -------------------------------------
-- 用户音色配置表：保存用户当前语音参数与音色选择
-- 表名：ts_user_voice_config
-- -------------------------------------
DROP TABLE IF EXISTS `ts_user_voice_config`;
CREATE TABLE ts_user_voice_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户ID',
    selected_voice_profile_id BIGINT UNSIGNED NOT NULL COMMENT '当前选中的音色ID',
    pitch_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '音调百分比，例如20表示+20%',
    speed_rate DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '语速倍数，例如1.2表示1.2x',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_selected_voice_profile_id (selected_voice_profile_id),
    CONSTRAINT fk_user_voice_config_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_voice_config_profile
        FOREIGN KEY (selected_voice_profile_id) REFERENCES ts_voice_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前音色配置表';

-- -------------------------------------
-- 示例数据（按需保留或删除）
-- -------------------------------------
INSERT INTO `ts_role_info` (`user_id`, `role_name`, `avatar_url`, `role_subtitle`, `gender`, `intro_text`, `ext_json`, `status`, `is_public`)
VALUES
(1, '用户', NULL, 'user', 'unknown', '默认用户角色', JSON_OBJECT('character_code', 'CHAR_USER', 'character_type', 'user', 'tags', JSON_ARRAY('默认', '主视角')), 1, 1),
(1, '赛博朋克黑客', NULL, 'main', 'male', '擅长潜入与数据破解的核心角色', JSON_OBJECT('character_code', 'CHAR_HACKER_001', 'character_type', 'main', 'tags', JSON_ARRAY('赛博朋克', '黑客', '主角')), 1, 1),
(1, '店长', NULL, 'npc', 'male', '负责门店经营与线索发布', JSON_OBJECT('character_code', 'CHAR_STORE_001', 'character_type', 'npc', 'tags', JSON_ARRAY('门店', 'NPC')), 1, 1);

INSERT INTO `ts_story_info` (`story_code`, `title`, `story_mode`, `story_setting`, `scene_id`, `scene_name_snapshot`, `status`, `is_ai_story_setting`, `is_ai_character`, `is_ai_outline`, `remark`, `created_by`, `created_name`, `updated_by`, `updated_name`, `is_deleted`)
VALUES
('STORY_20250319_001', '赛博门店迷局', 'chapter', '未来都市中，一家实体门店被卷入数据失窃事件，用户将与黑客角色协作追查真相。', 10001, '未来概念店', 0, 0, 0, 0, '示例故事草稿', 1, 'admin', 1, 'admin', 0);

INSERT INTO `ts_story_role_rel` (`story_id`, `role_id`, `role_type`, `sort_no`, `is_required`, `join_source`)
VALUES
(1, 1, 'user', 1, 1, 'manual'),
(1, 2, 'main', 2, 1, 'manual'),
(1, 3, 'npc', 3, 0, 'manual');

INSERT INTO `ts_story_chapter` (`story_id`, `chapter_no`, `chapter_title`, `chapter_desc`, `opening_content`, `opening_role_id`, `mission_target`, `status`, `is_ai_generated`, `sort_no`)
VALUES
(1, 1, '第一章', '描述主要情节，包括用户在故事中和其他角色的互动。', '夜幕降临，未来概念店的后门被悄悄打开，一条神秘讯息出现在终端上。', 2, '查明失窃数据的来源，并找到第一位关键证人。', 1, 0, 1),
(1, 2, '第二章', '随着调查推进，更多角色逐渐浮出水面。', '店长拿出一张陈旧的物流单，上面写着一个早已停运的仓库编号。', 3, '前往废弃仓库，确认物流单与失窃事件的关系。', 1, 0, 2);

INSERT INTO `ts_story_chapter_forbidden_role` (`story_id`, `chapter_id`, `role_id`)
VALUES
(1, 1, 3);

SET FOREIGN_KEY_CHECKS = 1;

