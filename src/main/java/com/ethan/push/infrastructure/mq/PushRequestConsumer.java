package com.ethan.push.infrastructure.mq;

import com.alibaba.fastjson.JSON;
import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.infrastructure.utils.TraceIdUtils;
import com.ethan.push.application.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 业务方直接投递 MQ 的消费者
 * 对应 Austin 的 "MQ 接入" 方式
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "austin_trigger", consumerGroup = "austin_trigger_group")
public class PushRequestConsumer implements RocketMQListener<Message> {

    @Autowired
    private SendService sendService;

    @Override
    public void onMessage(Message message) {
        // 1. 从 Header 中提取 TraceId 并放入 MDC
        String traceId = (String) message.getHeaders().get(TraceIdUtils.TRACE_ID);
        TraceIdUtils.setTraceId(traceId);

        try {
            String body = (String) message.getPayload();
            SendRequest sendRequest = JSON.parseObject(body, SendRequest.class);
            if (sendRequest != null) {
                sendService.send(sendRequest);
            }
        } catch (Exception e) {
            log.error("PushRequestConsumer error", e);
        } finally {
            // 清理 TraceId
            TraceIdUtils.removeTraceId();
        }
    }
}

