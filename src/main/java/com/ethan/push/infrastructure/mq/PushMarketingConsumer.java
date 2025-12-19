package com.ethan.push.infrastructure.mq;

import com.alibaba.fastjson.JSON;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.infrastructure.utils.TraceIdUtils;
import com.ethan.push.infrastructure.channel.HandlerHolder;
import com.ethan.push.domain.service.AggregationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.dromara.dynamictp.core.DtpRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 消费营销类 MQ 消息 (广告、推广、聚合消息)
 * 位于 L3 低优通道，允许堆积，使用 CallerRunsPolicy 反压
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${rocketmq.topic.push}", consumerGroup = "${rocketmq.producer.group}")
public class PushMarketingConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private HandlerHolder handlerHolder;
    @Autowired
    private AggregationService aggregationService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String DTP_MARKETING_EXECUTOR = "dtpMarketingExecutor";

    @Override
    public void onMessage(MessageExt message) {
        // 1. 从 Header 中提取 TraceId 并放入 MDC (RocketMQ 原生属性)
        String traceId = message.getUserProperty(TraceIdUtils.TRACE_ID);
        TraceIdUtils.setTraceId(traceId);

        // 2. 幂等性校验
        String msgId = message.getMsgId();
        if (msgId != null) {
            String idempotentKey = "push:idempotent:" + msgId;
            if (Boolean.FALSE.equals(stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES))) {
                log.warn("重复消息，幂等性校验失败，直接丢弃: MsgId={}", msgId);
                return;
            }
        }

        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            TaskInfo taskInfo = JSON.parseObject(body, TaskInfo.class);
            if (taskInfo == null || taskInfo.getMsgType() == null) {
                return;
            }

            // 检查是否是聚合触发消息 (聚合消息通常属于营销类)
            boolean isAggregation = MessageType.AGGREGATION.getCode().equals(taskInfo.getMsgType());
            TaskInfo triggerTaskInfo = isAggregation ? taskInfo : null;

            if (isAggregation) {
                // 获取聚合后的真实消息 (此时不删除 Redis)
                taskInfo = aggregationService.getAggregatedData(taskInfo);
                if (taskInfo == null) {
                    // 无数据或已过期
                    return;
                }
            }

            // 3. 线程池隔离执行任务
            final TaskInfo finalTaskInfo = taskInfo;
            Executor executor = DtpRegistry.getExecutor(DTP_MARKETING_EXECUTOR);
            
            // 捕获当前线程 TraceId，传递给子线程
            String currentTraceId = TraceIdUtils.getTraceId();

            executor.execute(() -> {
                // 子线程设置 TraceId
                TraceIdUtils.setTraceId(currentTraceId);
                try {
                    handlerHolder.route(finalTaskInfo.getSendChannel()).send(finalTaskInfo);
                    
                    // 如果是聚合消息，发送成功后需要清理 Redis
                    if (isAggregation) {
                        aggregationService.clear(triggerTaskInfo);
                    }
                } catch (Exception e) {
                    log.error("Marketing Task execution failed. TaskInfo: {}", JSON.toJSONString(finalTaskInfo), e);
                } finally {
                    // 子线程清理 TraceId
                    TraceIdUtils.removeTraceId();
                }
            });

        } catch (Exception e) {
            log.error("PushMarketingConsumer process error.", e);
            throw new RuntimeException(e);
        } finally {
            // 消费者主线程清理 TraceId
            TraceIdUtils.removeTraceId();
        }
    }
}

