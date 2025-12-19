package com.ethan.push.infrastructure.config;

import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.dromara.dynamictp.core.support.ThreadPoolCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    /**
     * 核心业务线程池
     * 用于处理 MQ 消费后的具体推送逻辑
     */
    @Bean("dtpCodeExecutor")
    public DtpExecutor dtpCodeExecutor() {
        return ThreadPoolCreator.createDynamicFast("dtpCodeExecutor");
    }

    /**
     * 营销业务线程池
     * 用于处理营销消息的 Pipeline 组装和发送
     */
    @Bean("dtpMarketingExecutor")
    public DtpExecutor dtpMarketingExecutor() {
        return ThreadPoolCreator.createDynamicFast("dtpMarketingExecutor");
    }

    /**
     * 通知业务线程池
     * 用于处理通知类消息的发送
     */
    @Bean("dtpNoticeExecutor")
    public DtpExecutor dtpNoticeExecutor() {
        return ThreadPoolCreator.createDynamicFast("dtpNoticeExecutor");
    }
}
