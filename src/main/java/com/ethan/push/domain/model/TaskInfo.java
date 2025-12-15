package com.ethan.push.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 核心任务信息
 * 贯穿 Controller -> Pipeline -> MQ -> Consumer -> Handler 的全链路
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskInfo implements Serializable {

    /**
     * 业务执行的唯一ID (用于追踪)
     */
    private String businessId;

    /**
     * 消息模板ID (关联数据库里的模板配置)
     */
    private Long messageTemplateId;

    /**
     * 发送渠道 (10:短信, 20:邮件, 30:APP推送)
     * 关联 ChannelType 枚举
     */
    private Integer sendChannel;

    /**
     * 消息类型 (10:验证码, 20:通知, 30:营销)
     * 关联 MessageType 枚举
     * 决定了去重策略和是否屏蔽
     */
    private Integer msgType;

    /**
     * 屏蔽类型 (10:不屏蔽, 20:夜间屏蔽)
     */
    private Integer shieldType;

    /**
     * 接收者集合 (手机号/邮箱/OpenId)
     */
    private Set<String> receiver;

    /**
     * 接收者ID类型 (10:手机号, 20:邮箱 ...)
     */
    private Integer idType;

    /**
     * 消息内容 (可能是JSON，也可能是纯文本，包含占位符)
     */
    private String content;

    /**
     * 动态参数 (用于替换 content 里的占位符)
     * Key: name -> Value: Ethan
     */
    private Map<String, String> params;

    /**
     * 扩展参数 (用于消息聚合等特殊场景)
     */
    private Map<String, String> extraMap;
}
