package com.ethan.push.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum MessageType {
    NOTICE(10, "通知类消息"),
    MARKETING(20, "营销类消息"),
    AUTH_CODE(30, "验证码消息"),
    AGGREGATION(40, "聚合消息"),
    ;

    private final Integer code;
    private final String description;
}
