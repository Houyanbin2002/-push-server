package com.ethan.push.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送请求参数 (API 入参)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendRequest {

    /**
     * 执行业务类型 (send:发送, recall:撤回)
     */
    private String code;

    /**
     * 消息模板ID
     */
    private Long messageTemplateId;

    /**
     * 消息参数列表 (支持批量发送，千人千面)
     */
    private List<MessageParam> messageParam;
}

