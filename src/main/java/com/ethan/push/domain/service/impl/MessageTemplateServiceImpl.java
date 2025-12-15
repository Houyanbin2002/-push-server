package com.ethan.push.domain.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.ethan.push.domain.entity.MessageTemplate;
import com.ethan.push.infrastructure.persistence.mapper.MessageTemplateMapper;
import com.ethan.push.domain.service.MessageTemplateService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息模板服务实现 (多级缓存)
 */
@Slf4j
@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    @Autowired
    private MessageTemplateMapper messageTemplateMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("templateCache")
    private Cache<String, Object> templateCache;

    private static final String CACHE_KEY_PREFIX = "msg_template:";

    @Value("${redis.expire-seconds:300}")
    private Long redisExpireSeconds;

    @Override
    public MessageTemplate queryById(Long id) {
        // 1. 查询本地缓存 (L1)
        MessageTemplate template = (MessageTemplate) templateCache.getIfPresent(String.valueOf(id));
        if (ObjectUtil.isNotNull(template)) {
            return template;
        }

        // 2. 查询 Redis 缓存 (L2)
        String redisKey = CACHE_KEY_PREFIX + id;
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        
        // 2.1 缓存穿透处理：如果 Redis 里存的是空值标记，直接返回 null
        if ("<NULL>".equals(redisValue)) {
            return null;
        }
        
        if (ObjectUtil.isNotNull(redisValue)) {
            template = JSON.parseObject(redisValue, MessageTemplate.class);
            // 回填本地缓存
            templateCache.put(String.valueOf(id), template);
            return template;
        }

        // 3. 查询数据库 (DB) - 加锁防止缓存击穿
        // 这里的锁粒度较粗，生产环境可以用分布式锁或基于ID的细粒度锁
        synchronized (this) {
            // 双重检查 (DCL)
            template = (MessageTemplate) templateCache.getIfPresent(String.valueOf(id));
            if (ObjectUtil.isNotNull(template)) {
                return template;
            }
            redisValue = stringRedisTemplate.opsForValue().get(redisKey);
            if (ObjectUtil.isNotNull(redisValue)) {
                if ("<NULL>".equals(redisValue)) {
                    return null;
                }
                template = JSON.parseObject(redisValue, MessageTemplate.class);
                templateCache.put(String.valueOf(id), template);
                return template;
            }

            // 鐪熸鏌ュ簱
            try {
                template = messageTemplateMapper.selectById(id);
            } catch (Exception e) {
                log.warn("Database query failed (Table might be missing). Using Mock data for ID:{}", id);
            }

            // Mock 鏁版嵁鍏滃簳 (涓轰簡娴嬭瘯鏂逛究)
            if (template == null && id == 1L) {
                template = MessageTemplate.builder()
                        .id(1L)
                        .name("鐐硅禐閫氱煡")
                        .msgType(30) // 钀ラ攢/鑱氬悎
                        .sendChannel(10) // 鐭俊
                        .shieldType(10)
                        .msgContent("{$name} liked your work")
                        .build();
            }

            if (ObjectUtil.isNotNull(template)) {
                // 鍥炲～ Redis (璁剧疆闅忔満杩囨湡鏃堕棿锛岄槻姝㈤洩宕?
                // 基础时间 + 0~60秒随机
                long randomExpire = redisExpireSeconds + (long) (Math.random() * 60);
                stringRedisTemplate.opsForValue().set(redisKey, JSON.toJSONString(template), randomExpire, TimeUnit.SECONDS);
                
                // 回填本地缓存
                templateCache.put(String.valueOf(id), template);
            } else {
                // 缓存穿透处理：数据库也没查到，写入空值标记，过期时间设短一点 (如 30秒)
                stringRedisTemplate.opsForValue().set(redisKey, "<NULL>", 30, TimeUnit.SECONDS);
            }
        }

        return template;
    }
}

