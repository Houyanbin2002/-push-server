package com.ethan.push.domain.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.domain.service.DeduplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * 频率去重服务实现
 */
@Slf4j
@Service
public class FrequencyDeduplicationService implements DeduplicationService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/limit.lua")));
        redisScript.setResultType(Long.class);
    }

    @Override
    public void deduplication(TaskInfo taskInfo) {
        // 1. 只有营销类消息需要做频率限制
        if (!MessageType.MARKETING.getCode().equals(taskInfo.getMsgType())) {
            return;
        }

        // 2. 获取接收者
        Set<String> receivers = taskInfo.getReceiver();
        if (CollUtil.isEmpty(receivers)) {
            return;
        }

        // 3. Lua 脚本限流
        // Key: frequency:marketing:userId
        // 假设限制: 5分钟内最多 3条 (实际应从配置获取)
        long windowTime = 5 * 60 * 1000L; // 5分钟
        int limitCount = 3;

        for (String receiver : receivers) {
            String key = "frequency:marketing:" + receiver;
            String member = UUID.randomUUID().toString();
            
            try {
                Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(key),
                        String.valueOf(System.currentTimeMillis()),
                        String.valueOf(windowTime),
                        String.valueOf(limitCount),
                        member);

                if (result != null && result == 0) {
                    // 触发限流，移除该接收者 (或者抛出异常中断整个任务，视业务而定)
                    // 这里简单处理：从接收者列表中移除
                    // 注意：直接修改 taskInfo.getReceiver() 可能会有并发修改异常，如果它是不可变集合
                    // 但通常 TaskInfo 里的 Set 是 HashSet，可以 remove
                    // 更好的做法是收集需要移除的，最后 removeAll
                    log.info("Trigger frequency deduplication, user: {}", receiver);
                    // 暂时抛出异常中断，或者在 Action 层处理
                    // 为了演示简单，这里我们抛出 RuntimeException，Action 会捕获并中断
                    throw new RuntimeException("Triggered frequency deduplication limit: " + receiver);
                }
            } catch (Exception e) {
                log.error("Frequency deduplication check error", e);
                throw e; // 鎶涘嚭寮傚父璁?Action 澶勭悊
            }
        }
    }
}


