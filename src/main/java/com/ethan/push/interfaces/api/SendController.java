package com.ethan.push.interfaces.api;

import com.alibaba.fastjson.JSON;
import com.ethan.push.application.service.SendService;
import com.ethan.push.domain.entity.MessageTemplate;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.domain.service.MessageTemplateService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@Slf4j
@RestController
public class SendController {

    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    @Resource
    private SendService sendService;

    @Resource
    private MessageTemplateService messageTemplateService;

    // 全局限流器，用于保护入口，防止内部DDOS
    @Resource
    private RedissonClient redissonClient;
    
    // 营销消息的缓冲 Topic (原 triggerTopic)
    @Value("${rocketmq.topic.push-trigger}")
    private String triggerTopic; 

    // 全局 QPS 限制，例如限制整个系统的入口 QPS 为 5000
    private static final String GLOBAL_LIMITER_KEY = "global_rate_limiter";
    private RRateLimiter globalRateLimiter;


    // 启动时初始化全局限流器
    @jakarta.annotation.PostConstruct
    public void initGlobalLimiter() {
        globalRateLimiter = redissonClient.getRateLimiter(GLOBAL_LIMITER_KEY);
        // 【测试模式】为了演示限流效果，将阈值临时改为 10 QPS
        // 正常生产环境应该是 5000 或更高
        globalRateLimiter.trySetRate(org.redisson.api.RateType.OVERALL, 10, 1, org.redisson.api.RateIntervalUnit.SECONDS);
        
        // 如果 Redis 里已经有旧配置(5000)，强制更新为 10 (仅供测试用)
        // 注意：生产环境不要这么写，应该由运维平台管理
        globalRateLimiter.setRate(org.redisson.api.RateType.OVERALL, 10, 1, org.redisson.api.RateIntervalUnit.SECONDS);
    }


    /**
     * 发送接口：接收请求后，立即异步推送到 MQ，实现削峰
     * @param sendRequest 发送请求体
     * @return 立即返回受理结果
     */
    @PostMapping("/send")
    public ResponseEntity<BasicResult> send(@RequestBody SendRequest sendRequest) {
        // 1. 全局防雪崩限流 (在 FlowControlConfig 中初始化)
        // if (globalRateLimiter != null && !globalRateLimiter.tryAcquire()) {
        //     log.warn("全局限流触发，请求被拒绝");
        //     return ResponseEntity.status(429).body(BasicResult.fail("系统繁忙，请稍后重试"));
        // }

        // 2. 入口分流：优先检查模板类型 (修复验证码被营销消息阻塞的问题)
        // 查询模板信息 (走本地缓存+Redis，速度很快)
        MessageTemplate messageTemplate = messageTemplateService.queryById(sendRequest.getMessageTemplateId());
        if (messageTemplate == null) {
            return ResponseEntity.badRequest().body(BasicResult.fail("模板不存在"));
        }

        // 如果是验证码(30)或通知类(10)，直接同步处理，不走 Trigger Topic 排队
        // 这样可以避开营销消息(20)在 Trigger Topic 中的积压
        if (MessageType.AUTH_CODE.getCode().equals(messageTemplate.getMsgType()) || 
            MessageType.NOTICE.getCode().equals(messageTemplate.getMsgType())) {
            
            // 同步调用 Service -> Pipeline -> SendMqAction -> Urgent/Notice Topic
            // 虽然这里是同步调用，但 Pipeline 里的 SendMqAction 还是异步发 MQ，所以整体响应很快
            BasicResult result = sendService.send(sendRequest);
            return ResponseEntity.ok(result);
        }
        
        // 3. 营销类消息(20)，继续走原来的异步削峰逻辑 (SendController -> Trigger Topic -> Consumer -> Pipeline)
        
        // 【关键修改】只对营销消息进行限流！
        // 验证码和通知类消息直接放行，确保重要消息不被误杀
        if (globalRateLimiter != null && !globalRateLimiter.tryAcquire()) {
            log.warn("营销消息限流触发，请求被拒绝");
            return ResponseEntity.status(429).body(BasicResult.fail("系统繁忙，请稍后重试"));
        }

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
        
        // 4. 立即返回受理成功，实现削峰
        return ResponseEntity.ok(BasicResult.success("请求已进入处理队列"));
    }
}

