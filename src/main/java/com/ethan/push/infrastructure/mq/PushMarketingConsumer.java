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
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 消费营销类 MQ 消息 (广告、推广、聚合消息)
 * 位于 L3 低优通道，允许堆积，使用 CallerRunsPolicy 反压
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "${rocketmq.topic.push}", consumerGroup = "${rocketmq.producer.group}")
public class PushMarketingConsumer implements RocketMQListener<Message> {

    @Autowired
    private HandlerHolder handlerHolder;
    @Autowired
    private AggregationService aggregationService;

    private static final String DTP_MARKETING_EXECUTOR = "dtp-marketing-executor";

    @Override
    public void onMessage(Message message) {
        // 1. 从 Header 中提取 TraceId 并放入 MDC
        String traceId = (String) message.getHeaders().get(TraceIdUtils.TRACE_ID);
        TraceIdUtils.setTraceId(traceId);

        try {
            String body = (String) message.getPayload();
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

            // 策略分流
            // 1. 聚合消息：同步处理 (保证可靠性，利用 MQ 重试机制)
            if (isAggregation) {
                try {
                    // 直接调用 Handler 发送 (阻塞直到完成)
                    boolean success = handlerHolder.route(taskInfo.getSendChannel()).send(taskInfo);
                    if (success) {
                        // 发送成功，ACK 清理 Redis
                        aggregationService.clear(triggerTaskInfo);
                    } else {
                        // 发送失败，抛出异常 MQ 重试
                        throw new RuntimeException("Aggregated message send failed");
                    }
                } catch (Exception e) {
                    log.error("Aggregated message send failed, waiting for retry. bizId:{}", taskInfo.getBusinessId(), e);
                    throw e; // 抛出异常，触发 MQ 重试
                }
                return; // 结束
            }

            // 2. 普通营销消息：异步处理 (追求吞吐量，允许 CallerRuns 反压)
            Executor executor = DtpRegistry.getExecutor(DTP_MARKETING_EXECUTOR);
            TaskInfo finalTaskInfo = taskInfo;
            
            // 捕获当前线程 TraceId，传递给子线程
            String currentTraceId = TraceIdUtils.getTraceId();
            
            executor.execute(() -> {
                // 子线程设置 TraceId
                TraceIdUtils.setTraceId(currentTraceId);
                try {
                    handlerHolder.route(finalTaskInfo.getSendChannel()).send(finalTaskInfo);
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

