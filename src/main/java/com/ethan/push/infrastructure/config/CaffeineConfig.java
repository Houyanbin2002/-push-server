package com.ethan.push.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 */
@Configuration
public class CaffeineConfig {

    @Value("${caffeine.expire-after-write:60}")
    private Integer expireAfterWrite;

    @Value("${caffeine.maximum-size:1000}")
    private Integer maximumSize;

    @Bean("templateCache")
    public Cache<String, Object> templateCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expireAfterWrite, TimeUnit.SECONDS)
                .maximumSize(maximumSize)
                .initialCapacity(100)
                .recordStats()
                .build();
    }
}

