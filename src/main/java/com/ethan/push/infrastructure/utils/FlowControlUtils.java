package com.ethan.push.infrastructure.utils;

import lombok.extern.slf4j.Slf4j;
// 移除 Guava 的导入
// import com.google.common.util.concurrent.RateLimiter; 
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlowControlUtils {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化限流器 (在 FlowControlConfig 里调用)
     * @param channelCode 渠道编码 (如 10=短信, 20=邮件)
     * @param qps 该渠道允许的最大 QPS (如 100)
     */
    public void initLimiter(Integer channelCode, Double qps) {
        // Key 格式：flow_control:10 (分布式共享)
        String key = "flow_control:" + channelCode;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        
        // RateType.OVERALL 表示所有客户端(分布式)共享限流
        // 每 1 秒产生 qps 个令牌 (这里的 qps 必须转成 long)
        // 尝试设置速率，如果已设置则返回 false
        boolean result = rateLimiter.trySetRate(RateType.OVERALL, qps.longValue(), 1, RateIntervalUnit.SECONDS);
        if (result) {
            log.info("分布式限流器初始化成功，Key={}，QPS={}", key, qps);
        } else {
            log.info("分布式限流器已存在，Key={}，无需重复初始化", key);
        }
    }

    /**
     * 阻塞式获取令牌 (用于 Handler，必须拿到令牌才能调用下游)
     */
    public void acquire(Integer channelCode) {
        String key = "flow_control:" + channelCode;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        
        // 阻塞获取 1 个令牌。如果令牌用完，线程会等待。
        rateLimiter.acquire(1); 
        // log.info("成功获取令牌，Key={}", key); // 调试时可以打开
    }
    
    /**
     * 非阻塞尝试获取 (备用，用于非核心限流)
     */
    public boolean tryAcquire(Integer channelCode) {
        String key = "flow_control:" + channelCode;
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        return rateLimiter.tryAcquire(1);
    }
}
