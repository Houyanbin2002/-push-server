package com.ethan.push.infrastructure.mq;

import com.alibaba.fastjson.JSON;
import com.ethan.push.application.service.SendService;
import com.ethan.push.interfaces.dto.SendRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.apache.rocketmq.common.message.MessageExt;
import org.dromara.dynamictp.core.executor.DtpExecutor; // 假设使用 DynamicTP

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${rocketmq.topic.push}",
        consumerGroup = "${rocketmq.producer.group}"
)
public class PushRequestConsumer implements RocketMQListener<MessageExt> {

    @Resource
    private SendService sendService;
    
    @Resource(name = "dtpCodeExecutor") // 注入你为核心业务配置的线程池
    private DtpExecutor dtpCodeExecutor; 
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public void onMessage(MessageExt message) {
        // 1. 幂等性校验 (核心！防止重复消费导致重复发短信)
        String msgId = message.getMsgId();
        String idempotentKey = "push:idempotent:" + msgId;

        // 5分钟内不允许重复消费
        if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES))) {
            log.warn("重复消息，幂等性校验失败，直接丢弃: MsgId={}", msgId);
            return;
        }

        // 2. 解析消息体
        String body = new String(message.getBody());
        SendRequest sendRequest = JSON.parseObject(body, SendRequest.class);

        // 3. 线程池隔离执行任务 (核心！释放 MQ 线程，防止阻塞拉取)
        // 使用动态线程池来执行业务逻辑，实现渠道隔离
        dtpCodeExecutor.execute(() -> {
            try {
                // sendService.send() 现在只包含责任链的业务逻辑
                sendService.send(sendRequest); 
            } catch (Exception e) {
                // 异常处理：记录日志。由于没有返回 ConsumeConcurrentlyStatus，RocketMQ 会自动根据异常重试。
                log.error("消费业务逻辑执行异常，MsgId:{}，请求体:{}", msgId, body, e);
            }
        });
    }
}

