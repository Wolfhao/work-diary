-- Work Diary (宠物博主商单管理工具) 数据库初始化脚本
-- 数据库名: work_diary (请按需自行创建)
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_general_ci

CREATE DATABASE IF NOT EXISTS `work_diary` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `work_diary`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表 (User)
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `open_id` varchar(64) NOT NULL COMMENT '微信OpenID',
  `union_id` varchar(64) DEFAULT NULL COMMENT '微信UnionID(预留)',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称(授权或自定义)',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(32) DEFAULT NULL COMMENT '绑定的手机号',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '状态(1:正常, 0:禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(2) NOT NULL DEFAULT '0' COMMENT '逻辑删除(0:未删, 1:已删)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信小程序用户表';

-- ----------------------------
-- 2. 商单表 (Work Order)
-- ----------------------------
DROP TABLE IF EXISTS `work_order`;
CREATE TABLE `work_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID(关联user.id)',
  `title` varchar(128) NOT NULL COMMENT '商单名称/合作项目名',
  `description` text COMMENT '商单描述/要求备注',
  `platform` varchar(64) DEFAULT NULL COMMENT '发布平台(例如:小红书,抖音,B站,微博)',
  `image_urls` json DEFAULT NULL COMMENT '商单相关截图URL集合(JSON数组)',
  
  -- 垫付资金相关
  `advance_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '需要垫付的金额(0代表无需垫付)',
  `is_advance_recovered` tinyint(2) NOT NULL DEFAULT '0' COMMENT '垫付是否已收回(0:未收回, 1:已收回)',
  `advance_recover_time` datetime DEFAULT NULL COMMENT '垫付实际收回的时间',
  
  -- 收入资金相关
  `income_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '商单收入/酬金',
  `is_income_received` tinyint(2) NOT NULL DEFAULT '0' COMMENT '收入是否已到账(0:未到账, 1:已到账)',
  `income_receive_time` datetime DEFAULT NULL COMMENT '收入实际到账的时间',
  
  -- 整体进度
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '商单状态(1:沟通中/待执行, 2:执行中/创作中, 3:待结算/待回款, 4:已完成, 9:已取消)',
  
  -- 通用审计字段
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(2) NOT NULL DEFAULT '0' COMMENT '逻辑删除(0:未删, 1:已删)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商单记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- 初始设计说明:
-- 1. 所有表增加 is_deleted 作为逻辑删除标识，适用于 MyBatis-Plus 的 @TableLogic。
-- 2. 金额单位统一采用 decimal(10,2)，精确到分且带小数，方便直接展示和普通计算，避免小程序端处理分时的乘除麻烦。
-- 3. 图片集合采用 JSON 格式存储，方便多图扩展。
-- 4. 增加了 platform 平台字段方便博主标记这单要发在哪里。
