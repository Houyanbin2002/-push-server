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
 * 消费加急 MQ 消息 (验证码)
 * 独立 Topic，独立 Consumer，确保不被营销消息阻塞
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${rocketmq.topic.push-urgent}", consumerGroup = "${rocketmq.producer.group}-urgent")
public class PushUrgentConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private HandlerHolder handlerHolder;

    private static final String DTP_CODE_EXECUTOR = "dtpCodeExecutor";

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        TaskInfo taskInfo = JSON.parseObject(body, TaskInfo.class);
        if (taskInfo == null) {
            return;
        }

        try {
            // 直接使用高优先级线程池
            Executor executor = DtpRegistry.getExecutor(DTP_CODE_EXECUTOR);

            executor.execute(() -> {
                try {
                    handlerHolder.route(taskInfo.getSendChannel()).send(taskInfo);
                } catch (Exception e) {
                    log.error("Urgent Task execution failed. TaskInfo: {}", JSON.toJSONString(taskInfo), e);
                }
            });

        } catch (Exception e) {
            log.error("PushUrgentConsumer process error. message: {}", message, e);
        }
    }
}

