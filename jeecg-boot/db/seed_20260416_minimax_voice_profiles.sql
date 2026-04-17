START TRANSACTION;

-- 0) 兼容旧库：若 ts_voice_profile 还没有 provider_voice_id，则补上
SET @db_name := DATABASE();
SET @has_provider_col := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'ts_voice_profile'
    AND COLUMN_NAME = 'provider_voice_id'
);

SET @ddl_sql := IF(
  @has_provider_col = 0,
  'ALTER TABLE `ts_voice_profile` ADD COLUMN `provider_voice_id` VARCHAR(100) NULL COMMENT ''TTS provider voice id'' AFTER `name`',
  'SELECT ''provider_voice_id exists'' AS msg'
);

PREPARE stmt FROM @ddl_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1) 音色种子数据（按 providerVoiceId 作为唯一业务键）
DROP TEMPORARY TABLE IF EXISTS tmp_voice_seed;
CREATE TEMPORARY TABLE tmp_voice_seed (
  display_name VARCHAR(50) NOT NULL,
  gender ENUM('unknown', 'male', 'female') NOT NULL,
  provider_voice_id VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL,
  age_group ENUM('child','teen','young','adult','middle','senior') NOT NULL DEFAULT 'adult',
  PRIMARY KEY (provider_voice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_voice_seed (display_name, gender, provider_voice_id, sort_no, age_group) VALUES
('知性女声',       'female', 'Chinese (Mandarin)_Mature_Woman',       10, 'adult'),
('成熟甜美女声',   'female', 'Chinese (Mandarin)_Sweet_Lady',          20, 'adult'),
('可靠男声',       'male',   'Chinese (Mandarin)_Reliable_Executive',  30, 'adult'),
('绅士男声',       'male',   'Chinese (Mandarin)_Gentleman',           40, 'adult'),
('真诚成人声',     'male',   'Chinese (Mandarin)_Sincere_Adult',       50, 'adult'),
('温暖陪伴女声',   'female', 'Chinese (Mandarin)_Warm_Bestie',         60, 'adult'),
('睿智女声',       'female', 'Chinese (Mandarin)_Wise_Women',          70, 'adult'),
('温柔前辈音',     'male',   'Chinese (Mandarin)_Gentle_Senior',       80, 'adult');

-- 2) 先更新已有，再插入缺失
UPDATE ts_voice_profile p
JOIN tmp_voice_seed s ON p.provider_voice_id = s.provider_voice_id
SET p.name       = s.display_name,
    p.gender     = s.gender,
    p.age_group  = s.age_group,
    p.status     = 1,
    p.sort_no    = s.sort_no,
    p.updated_at = NOW();

INSERT INTO ts_voice_profile
(name, provider_voice_id, avatar_url, gender, age_group, status, sort_no, created_at, updated_at)
SELECT
  s.display_name, s.provider_voice_id, NULL, s.gender, s.age_group, 1, s.sort_no, NOW(), NOW()
FROM tmp_voice_seed s
LEFT JOIN ts_voice_profile p ON p.provider_voice_id = s.provider_voice_id
WHERE p.id IS NULL;

-- 3) 标签种子
INSERT INTO ts_voice_tag (tag_name, created_at) VALUES
('知性', NOW()),
('温柔', NOW()),
('亲和', NOW()),
('陪伴', NOW()),
('稳重', NOW()),
('成熟', NOW()),
('绅士', NOW()),
('真诚', NOW()),
('日常', NOW()),
('温暖', NOW()),
('安抚', NOW())
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- 4) 音色-标签关系
DROP TEMPORARY TABLE IF EXISTS tmp_voice_tag_seed;
CREATE TEMPORARY TABLE tmp_voice_tag_seed (
  provider_voice_id VARCHAR(100) NOT NULL,
  tag_name VARCHAR(30) NOT NULL,
  PRIMARY KEY (provider_voice_id, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tmp_voice_tag_seed (provider_voice_id, tag_name) VALUES
('Chinese (Mandarin)_Mature_Woman',      '知性'),
('Chinese (Mandarin)_Mature_Woman',      '温柔'),
('Chinese (Mandarin)_Sweet_Lady',        '亲和'),
('Chinese (Mandarin)_Sweet_Lady',        '陪伴'),
('Chinese (Mandarin)_Reliable_Executive','稳重'),
('Chinese (Mandarin)_Reliable_Executive','成熟'),
('Chinese (Mandarin)_Gentleman',         '温柔'),
('Chinese (Mandarin)_Gentleman',         '绅士'),
('Chinese (Mandarin)_Sincere_Adult',     '真诚'),
('Chinese (Mandarin)_Sincere_Adult',     '日常'),
('Chinese (Mandarin)_Warm_Bestie',       '温暖'),
('Chinese (Mandarin)_Warm_Bestie',       '安抚'),
('Chinese (Mandarin)_Wise_Women',        '知性'),
('Chinese (Mandarin)_Wise_Women',        '成熟'),
('Chinese (Mandarin)_Gentle_Senior',     '稳重'),
('Chinese (Mandarin)_Gentle_Senior',     '安抚');

INSERT INTO ts_voice_profile_tag (voice_profile_id, tag_id)
SELECT p.id, t.id
FROM tmp_voice_tag_seed m
JOIN ts_voice_profile p ON p.provider_voice_id = m.provider_voice_id
JOIN ts_voice_tag t ON t.tag_name = m.tag_name
LEFT JOIN ts_voice_profile_tag r
  ON r.voice_profile_id = p.id AND r.tag_id = t.id
WHERE r.id IS NULL;

COMMIT;

-- 5) 验证查询
SELECT id, name, provider_voice_id, gender, age_group, status, sort_no
FROM ts_voice_profile
WHERE provider_voice_id IN (
  'Chinese (Mandarin)_Mature_Woman',
  'Chinese (Mandarin)_Sweet_Lady',
  'Chinese (Mandarin)_Reliable_Executive',
  'Chinese (Mandarin)_Gentleman',
  'Chinese (Mandarin)_Sincere_Adult',
  'Chinese (Mandarin)_Warm_Bestie',
  'Chinese (Mandarin)_Wise_Women',
  'Chinese (Mandarin)_Gentle_Senior'
)
ORDER BY sort_no ASC;

SELECT p.name AS voice_name, t.tag_name
FROM ts_voice_profile p
JOIN ts_voice_profile_tag pt ON pt.voice_profile_id = p.id
JOIN ts_voice_tag t ON t.id = pt.tag_id
WHERE p.provider_voice_id IN (
  'Chinese (Mandarin)_Mature_Woman',
  'Chinese (Mandarin)_Sweet_Lady',
  'Chinese (Mandarin)_Reliable_Executive',
  'Chinese (Mandarin)_Gentleman',
  'Chinese (Mandarin)_Sincere_Adult',
  'Chinese (Mandarin)_Warm_Bestie',
  'Chinese (Mandarin)_Wise_Women',
  'Chinese (Mandarin)_Gentle_Senior'
)
ORDER BY p.sort_no, t.tag_name;
