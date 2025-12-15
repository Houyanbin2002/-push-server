package com.ethan.push.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum ChannelType {
    IM(10, "IM(站内信)"),
    PUSH(20, "push(通知栏)"),
    SMS(30, "sms(短信)"),
    EMAIL(40, "email(邮件)"),
    OFFICIAL_ACCOUNT(50, "OfficialAccounts(微信服务号)"),
    MINI_PROGRAM(60, "MiniProgram(微信小程序)"),
    ;

    private final Integer code;
    private final String description;
}
