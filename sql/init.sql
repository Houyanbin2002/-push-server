CREATE DATABASE IF NOT EXISTS `push_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `push_db`;

CREATE TABLE IF NOT EXISTS `message_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '标题',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '当前消息状态：0.待审核 10.审核失败 20.审核成功 30.被删除',
  `flow_id` varchar(50) DEFAULT NULL COMMENT '工单ID',
  `msg_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '当前消息状态：10.新建 20.停用 30.启用 40.等待发送 50.发送中 60.发送成功 70.发送失败',
  `id_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'id类型：10.userId 20.did 30.手机号 40.openId 50.email',
  `send_channel` tinyint(4) NOT NULL DEFAULT '0' COMMENT '发送渠道：10.IM 20.Push 30.短信 40.Email 50.公众号 60.小程序 70.企业微信',
  `template_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '模板类型：10.运营类 20.技术类接口调用',
  `msg_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息类型：10.通知类消息 20.营销类消息 30.验证码类消息',
  `shield_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '屏蔽类型：10.夜间不屏蔽 20.夜间屏蔽 30.夜间屏蔽(次日早上9点发送)',
  `msg_content` varchar(600) NOT NULL DEFAULT '' COMMENT '消息内容 占位符用{$var}表示',
  `send_account` tinyint(4) NOT NULL DEFAULT '0' COMMENT '发送账号 一个渠道下可存在多个账号',
  `creator` varchar(45) NOT NULL DEFAULT '' COMMENT '创建者',
  `updator` varchar(45) NOT NULL DEFAULT '' COMMENT '更新者',
  `auditor` varchar(45) NOT NULL DEFAULT '' COMMENT '审核人',
  `team` varchar(45) NOT NULL DEFAULT '' COMMENT '业务方团队',
  `proposer` varchar(45) NOT NULL DEFAULT '' COMMENT '业务方',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除：0.不删除 1.删除',
  `created` int(11) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `updated` int(11) NOT NULL DEFAULT '0' COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_channel` (`send_channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息模板信息';

INSERT INTO `message_template` (id, 
ame, udit_status, low_id, msg_status, id_type, send_channel, 	emplate_type, msg_type, shield_type, msg_content, send_account, creator, updator, uditor, 	eam, proposer, is_deleted, created, updated) VALUES (1, '����ģ��', 20, 'flow_123', 30, 30, 30, 10, 10, 10, '{"content":"{}�����ã����ǲ�����Ϣ"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000);

INSERT INTO `message_template` (id, 
ame, udit_status, low_id, msg_status, id_type, send_channel, 	emplate_type, msg_type, shield_type, msg_content, send_account, creator, updator, uditor, 	eam, proposer, is_deleted, created, updated) VALUES (2, '��֤��ģ��', 20, 'flow_code', 30, 30, 30, 40, 30, 10, '{"content":"������֤����{}"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000);
INSERT INTO `message_template` (id, 
ame, udit_status, low_id, msg_status, id_type, send_channel, 	emplate_type, msg_type, shield_type, msg_content, send_account, creator, updator, uditor, 	eam, proposer, is_deleted, created, updated) VALUES (3, 'Ӫ��ģ��', 20, 'flow_market', 30, 30, 30, 30, 20, 10, '{"content":"˫11��٣�ȫ��5�ۣ�"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000);
INSERT INTO `message_template` (id, 
ame, udit_status, low_id, msg_status, id_type, send_channel, 	emplate_type, msg_type, shield_type, msg_content, send_account, creator, updator, uditor, 	eam, proposer, is_deleted, created, updated) VALUES (4, 'ҹ������ģ��', 20, 'flow_shield', 30, 30, 30, 10, 10, 20, '{"content":"����һ��ҹ��������Ϣ"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000);
