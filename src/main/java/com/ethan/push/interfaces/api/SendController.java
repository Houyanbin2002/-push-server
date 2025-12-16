package com.ethan.push.interfaces.api;

import com.alibaba.fastjson.JSON;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.infrastructure.utils.TraceIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@Slf4j
@RestController
public class SendController {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    // 全局限流器，用于保护入口，防止内部DDOS
    @Resource
    private RedissonClient redissonClient;
    
    // 你在 application.yml 里配置的触发 Topic
    @Value("${rocketmq.topic.push}")
    private String triggerTopic; 

    // 全局 QPS 限制，例如限制整个系统的入口 QPS 为 5000
    private static final String GLOBAL_LIMITER_KEY = "global_rate_limiter";
    private RRateLimiter globalRateLimiter;


    // 启动时初始化全局限流器
    @jakarta.annotation.PostConstruct
    public void initGlobalLimiter() {
        globalRateLimiter = redissonClient.getRateLimiter(GLOBAL_LIMITER_KEY);
        // 初始化令牌桶规则：每秒 5000 个令牌 (如果未设置过)
        // RateType.OVERALL 表示所有客户端共享限制
        globalRateLimiter.trySetRate(org.redisson.api.RateType.OVERALL, 5000, 1, org.redisson.api.RateIntervalUnit.SECONDS);
    }


    /**
     * 发送接口：接收请求后，立即异步推送到 MQ，实现削峰
     * @param sendRequest 发送请求体
     * @return 立即返回受理结果
     */
    @PostMapping("/send")
    public BasicResult send(@RequestBody SendRequest sendRequest) {
        // 1. 全局防雪崩限流 (在 FlowControlConfig 中初始化)
        if (globalRateLimiter != null && !globalRateLimiter.tryAcquire()) {
            log.warn("全局限流触发，请求被拒绝");
            return BasicResult.fail("系统繁忙，请稍后重试");
        }
        
        // 2. 将请求体序列化后，异步发送到 MQ
        String payload = JSON.toJSONString(sendRequest);
        String traceId = TraceIdUtils.getTraceId();
        
        rocketMQTemplate.asyncSend(triggerTopic, payload, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("TraceId:{} 消息发送成功, MsgId:{}", traceId, sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                // 如果发送到 MQ 失败，记录日志并报警，这里需要告警机制介入
                log.error("TraceId:{} 消息发送失败，请求体:{}", traceId, payload, e);
            }
        });
        
        // 3. 立即返回受理成功，实现削峰
        return BasicResult.success("请求已进入处理队列");
    }
}

