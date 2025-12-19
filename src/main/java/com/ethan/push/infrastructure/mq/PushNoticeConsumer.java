package com.ethan.push.infrastructure.mq;

import com.alibaba.fastjson.JSON;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.infrastructure.channel.HandlerHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.dromara.dynamictp.core.DtpRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * 消费通知类 MQ 消息 (订单状态、物流信息等)
 * 独立 Topic，确保不被营销消息阻塞，同时优先级低于验证码
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${rocketmq.topic.push-notice}", consumerGroup = "${rocketmq.producer.group}-notice")
public class PushNoticeConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private HandlerHolder handlerHolder;

    private static final String DTP_NOTICE_EXECUTOR = "dtpNoticeExecutor";

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        TaskInfo taskInfo = JSON.parseObject(body, TaskInfo.class);
        if (taskInfo == null) {
            return;
        }

        try {
            // 使用通知类专用线程池
            Executor executor = DtpRegistry.getExecutor(DTP_NOTICE_EXECUTOR);

            executor.execute(() -> {
                try {
                    handlerHolder.route(taskInfo.getSendChannel()).send(taskInfo);
                } catch (Exception e) {
                    log.error("Notice Task execution failed. TaskInfo: {}", JSON.toJSONString(taskInfo), e);
                }
            });

        } catch (Exception e) {
            log.error("PushNoticeConsumer process error. message: {}", message, e);
        }
    }
}

