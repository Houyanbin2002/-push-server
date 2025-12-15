package com.ethan.push.infrastructure.utils;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FlowControlUtils {

    private final Map<Integer, RateLimiter> flowControlMap = new ConcurrentHashMap<>();

    public void initLimiter(Integer channelCode, Double qps) {
        if (flowControlMap.containsKey(channelCode)) {
            flowControlMap.get(channelCode).setRate(qps);
        } else {
            flowControlMap.put(channelCode, RateLimiter.create(qps));
        }
    }

    public boolean tryAcquire(Integer channelCode) {
        RateLimiter rateLimiter = flowControlMap.get(channelCode);
        if (rateLimiter == null) {
            return true;
        }
        return rateLimiter.tryAcquire();
    }

    public void acquire(Integer channelCode) {
        RateLimiter rateLimiter = flowControlMap.get(channelCode);
        if (rateLimiter == null) {
            return;
        }
        rateLimiter.acquire();
    }
}
