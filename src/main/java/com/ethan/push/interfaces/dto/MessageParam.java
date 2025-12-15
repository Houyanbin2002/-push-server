package com.ethan.push.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 消息参数
 * 对应一个具体的接收者和他的变量
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageParam {

    /**
     * 接收者 (手机号/邮箱/设备ID)
     */
    private String receiver;

    /**
     * 变量参数 (用于替换模板中的占位符)
     * e.g. {"name": "张三", "code": "1234"}
     */
    private Map<String, String> variables;

    /**
     * 扩展参数
     */
    private Map<String, String> extra;
}

