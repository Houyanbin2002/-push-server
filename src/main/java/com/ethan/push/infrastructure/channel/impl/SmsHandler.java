package com.ethan.push.infrastructure.channel.impl;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.ChannelType;
import com.ethan.push.infrastructure.channel.BaseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信发送处理器
 */
@Slf4j
@Component
public class SmsHandler extends BaseHandler {

    @Override
    public Integer getChannelCode() {
        return ChannelType.SMS.getCode();
    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        // 模拟调用阿里云/腾讯云短信接口
        log.info("SmsHandler 发送短信成功 userId:{}, content:{}", taskInfo.getReceiver(), taskInfo.getContent());
        return true;
    }
}

