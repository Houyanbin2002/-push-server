package com.ethan.push.infrastructure.channel.impl;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.ChannelType;
import com.ethan.push.infrastructure.channel.BaseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件发送处理器
 */
@Slf4j
@Component
public class EmailHandler extends BaseHandler {

    @Override
    public Integer getChannelCode() {
        return ChannelType.EMAIL.getCode();
    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        // 模拟调用 JavaMailSender 发送邮件
        log.info("EmailHandler 发送邮件成功 userId:{}, content:{}", taskInfo.getReceiver(), taskInfo.getContent());
        return true;
    }
}

