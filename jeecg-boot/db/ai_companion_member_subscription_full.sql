-- AI伴侣会员订阅系统初始化 SQL
-- MySQL 5.7+

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS member_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '会员名称',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT 'PRO/ULTRA',
    description VARCHAR(255),
    theme_color VARCHAR(50),
    status TINYINT DEFAULT 1,
    sort INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='会员等级';


CREATE TABLE IF NOT EXISTS member_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    cycle_type VARCHAR(20) NOT NULL COMMENT 'WEEK/MONTH/QUARTER/YEAR',
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    discount_text VARCHAR(100),
    is_recommend TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='会员套餐';


CREATE TABLE IF NOT EXISTS member_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(255),
    category VARCHAR(50),
    sort INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='会员权益定义';


CREATE TABLE IF NOT EXISTS member_plan_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    benefit_id BIGINT NOT NULL,
    value VARCHAR(100),
    unit VARCHAR(50),
    limit_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_member_plan_benefit (plan_id, benefit_id)
) COMMENT='会员权益关联';


CREATE TABLE IF NOT EXISTS member_gift (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    name VARCHAR(100),
    description VARCHAR(255),
    icon VARCHAR(255),
    sort INT DEFAULT 0
) COMMENT='会员开通赠礼';


CREATE TABLE IF NOT EXISTS user_membership (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL,
    plan_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    status TINYINT DEFAULT 1,
    auto_renew TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_membership_current (user_id, status, end_time)
) COMMENT='用户会员';


CREATE TABLE IF NOT EXISTS member_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL,
    product_id BIGINT NOT NULL,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    amount DECIMAL(10,2),
    payment_channel VARCHAR(50),
    provider VARCHAR(32) COMMENT '真实支付渠道',
    transaction_id VARCHAR(128) COMMENT '渠道交易ID',
    payment_status VARCHAR(32) NOT NULL DEFAULT 'CREATED'
        COMMENT 'CREATED/CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    callback_time DATETIME COMMENT '支付回调处理时间',
    status TINYINT DEFAULT 0 COMMENT '0待支付 1成功 2退款',
    pay_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_member_order_user (user_id, created_at),
    KEY idx_member_order_provider_transaction (provider, transaction_id)
) COMMENT='会员订单';

CREATE TABLE IF NOT EXISTS payment_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '会员订单ID',
    provider VARCHAR(32) NOT NULL COMMENT 'STRIPE/PAYPAL',
    transaction_id VARCHAR(128) COMMENT '渠道交易ID',
    payment_intent_id VARCHAR(128) COMMENT '渠道支付意图或支付订单ID',
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(32) NOT NULL COMMENT 'CREATING/PENDING/SUCCEEDED/FAILED/CANCELED',
    raw_response LONGTEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payment_transaction_order (order_id, id),
    UNIQUE KEY uk_payment_provider_intent (provider, payment_intent_id),
    UNIQUE KEY uk_payment_provider_transaction (provider, transaction_id)
) COMMENT='第三方支付流水';


CREATE TABLE IF NOT EXISTS user_benefit_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL,
    benefit_code VARCHAR(50) NOT NULL,
    total_amount INT DEFAULT 0,
    used_amount INT DEFAULT 0,
    expire_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_benefit_quota (user_id, benefit_code)
) COMMENT='用户权益额度';


CREATE TABLE IF NOT EXISTS benefit_usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(32) NOT NULL,
    benefit_code VARCHAR(50),
    consume_amount INT DEFAULT 1,
    biz_type VARCHAR(50),
    biz_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_benefit_usage_biz (user_id, benefit_code, biz_type, biz_id)
) COMMENT='权益消耗记录';


-- 会员等级
INSERT INTO member_plan(name,code,description,theme_color,sort)
VALUES
('PRO','PRO','深度陪伴会员','#9BFE03',1),
('ULTRA','ULTRA','完整AI世界会员','#8B5CFF',2);


-- 套餐
INSERT INTO member_product(plan_id,cycle_type,price,original_price,discount_text,is_recommend)
VALUES
(1,'WEEK',15,19,'',0),
(1,'MONTH',49,69,'推荐',1),
(1,'QUARTER',128,168,'节省20%',0),
(1,'YEAR',468,588,'节省40%',0),

(2,'WEEK',39,49,'',0),
(2,'MONTH',129,159,'推荐',1),
(2,'QUARTER',318,399,'节省20%',0),
(2,'YEAR',1188,1499,'节省30%',0);


-- 权益
INSERT INTO member_benefit(code,name,description,category,sort)
VALUES
('memory','长期记忆','记住你的故事、习惯和重要时刻','AI',1),
('chat_model','高级对话体验','更强模型，更自然情绪交流','AI',2),
('relationship','关系成长系统','好感度成长和专属互动','AI',3),
('role_create','角色创造增强','更多角色创建和形象生成额度','CREATE',4),
('voice','AI声音互动','高质量语音陪伴体验','VOICE',5),
('story','故事互动','长剧情模式和世界创作','STORY',6),
('3d_avatar','3D伴侣','VRM角色、动作和表情能力','3D',7),
('world_create','世界创造','创建专属AI世界','WORLD',8),
('voice_custom','专属声音定制','角色专属声音能力','VOICE',9),
('priority','高峰期优先体验','优先使用高级服务','SYSTEM',10);


-- PRO权益
INSERT INTO member_plan_benefit(plan_id,benefit_id,value,unit,limit_type)
VALUES
(1,1,'5000','条','LIMIT'),
(1,2,'开启','','ENABLE'),
(1,3,'开启','','ENABLE'),
(1,4,'更多','次','MONTH'),
(1,5,'100','次/月','LIMIT'),
(1,6,'无限','','ENABLE');


-- ULTRA权益
INSERT INTO member_plan_benefit(plan_id,benefit_id,value,unit,limit_type)
VALUES
(2,1,'无限','','ENABLE'),
(2,2,'最高优先','','ENABLE'),
(2,3,'全部阶段','','ENABLE'),
(2,4,'无限','','ENABLE'),
(2,5,'高级定制','','ENABLE'),
(2,6,'完整世界','','ENABLE'),
(2,7,'开启','','ENABLE'),
(2,8,'无限','','ENABLE'),
(2,9,'开启','','ENABLE'),
(2,10,'开启','','ENABLE');


-- 开通赠礼
INSERT INTO member_gift(plan_id,name,description,sort)
VALUES
(1,'5000对话额度','开通赠送AI对话额度',1),
(1,'专属角色皮肤','解锁会员专属外观',2),
(1,'高级声音体验','体验高级语音能力',3),
(1,'记忆扩展包','增加角色记忆容量',4),

(2,'3D伴侣体验','体验高级3D角色',1),
(2,'专属声音包','解锁定制声音',2),
(2,'世界创造礼包','创建专属世界',3),
(2,'超大记忆空间','永久扩展记忆能力',4);
