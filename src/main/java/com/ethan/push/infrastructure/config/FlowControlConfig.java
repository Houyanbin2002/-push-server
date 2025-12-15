package com.ethan.push.infrastructure.config;

import com.ethan.push.domain.enums.ChannelType;
import com.ethan.push.infrastructure.utils.FlowControlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 限流配置类
 * 初始化各渠道的 QPS 限制
 */
@Configuration
public class FlowControlConfig {

    @Autowired
    private FlowControlUtils flowControlUtils;

    @PostConstruct
    public void init() {
        // 1. 短信渠道 (SMS): 假设第三方限制 100 QPS
        flowControlUtils.initLimiter(ChannelType.SMS.getCode(), 100.0);

        // 2. 邮件渠道 (EMAIL): 假设邮件服务器较慢，限制 50 QPS
        flowControlUtils.initLimiter(ChannelType.EMAIL.getCode(), 50.0);

        // 3. APP推送 (PUSH): 假设厂商通道限制 500 QPS
        // flowControlUtils.initLimiter(ChannelType.PUSH.getCode(), 500.0);
        
        // ... 鍏朵粬娓犻亾閰嶇疆
    }
}

