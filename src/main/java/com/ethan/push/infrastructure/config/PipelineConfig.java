package com.ethan.push.infrastructure.config;

import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessController;
import com.ethan.push.domain.pipeline.impl.AssembleAction;
import com.ethan.push.domain.pipeline.impl.AggregationAction;
import com.ethan.push.domain.pipeline.impl.DeduplicationAction;
import com.ethan.push.domain.pipeline.impl.PreCheckAction;
import com.ethan.push.domain.pipeline.impl.SendMqAction;
import com.ethan.push.domain.pipeline.impl.ShieldAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline 配置类
 * 负责组装责任链
 */
@Configuration
public class PipelineConfig {

    @Autowired
    private AssembleAction assembleAction;
    @Autowired
    private DeduplicationAction deduplicationAction;
    @Autowired
    private AggregationAction aggregationAction;
    @Autowired
    private SendMqAction sendMqAction;
    @Autowired
    private PreCheckAction preCheckAction;
    @Autowired
    private ShieldAction shieldAction;

    /**
     * 缁勮 "send" 娴佺▼鐨勮矗浠婚摼
     * 顺序：前置校验(限流) -> 组装参数 -> 屏蔽 -> 去重/限流 -> 聚合 -> 发送MQ
     */
    @Bean
    public Map<String, List<BusinessProcess>> templateConfig() {
        Map<String, List<BusinessProcess>> config = new HashMap<>();
        config.put("send", Arrays.asList(preCheckAction, assembleAction, shieldAction, deduplicationAction, aggregationAction, sendMqAction));
        return config;
    }
    
    @Bean
    public ProcessController processController(Map<String, List<BusinessProcess>> templateConfig) {
        ProcessController controller = new ProcessController();
        controller.setTemplateConfig(templateConfig);
        return controller;
    }
}

