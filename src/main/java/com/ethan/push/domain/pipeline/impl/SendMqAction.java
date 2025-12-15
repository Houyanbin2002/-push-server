package com.ethan.push.domain.pipeline.impl;

import com.alibaba.fastjson.JSON;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.infrastructure.utils.TraceIdUtils;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 发送MQ Action
 * 将组装好的 TaskInfo 发送到 RocketMQ
 */
@Slf4j
@Service
public class SendMqAction implements BusinessProcess {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.push}")
    private String topic;

    @Value("${rocketmq.topic.push-urgent}")
    private String urgentTopic;

    @Value("${rocketmq.topic.push-notice}")
    private String noticeTopic;

    @Override
    public void process(ProcessContext context) {
        TaskInfo taskInfo = context.getTaskInfo();
        log.info("Pipeline: SendMqAction start. businessId:{}", taskInfo.getBusinessId());
        try {
            String destination = topic;
            // 1. 验证码 -> 加急通道
            if (MessageType.AUTH_CODE.getCode().equals(taskInfo.getMsgType())) {
                destination = urgentTopic;
            }
            // 2. 通知类 -> 通知通道 (新增)
            else if (MessageType.NOTICE.getCode().equals(taskInfo.getMsgType())) {
                destination = noticeTopic;
            }
            // 3. 其他(营销) -> 默认通道

            // 异步发送消息 (将 TraceId 放入 Header 透传)
            String jsonString = JSON.toJSONString(taskInfo);
            rocketMQTemplate.asyncSend(destination, 
                    MessageBuilder.withPayload(jsonString)
                            .setHeader(TraceIdUtils.TRACE_ID, TraceIdUtils.getTraceId())
                            .build(), 
                    new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("Async send mq success! businessId:{}", taskInfo.getBusinessId());
                }

                @Override
                public void onException(Throwable throwable) {
                    log.error("Async send mq fail! businessId:{}", taskInfo.getBusinessId(), throwable);
                }
            });
            
            context.setResponse(BasicResult.success());
        } catch (Exception e) {
            context.setNeedBreak(true);
            context.setResponse(BasicResult.fail("发送MQ失败: " + e.getMessage()));
            log.error("send mq fail! taskInfo:{}", JSON.toJSONString(taskInfo), e);
        }
    }
}

