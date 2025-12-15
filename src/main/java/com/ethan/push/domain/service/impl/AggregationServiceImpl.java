package com.ethan.push.domain.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.domain.service.AggregationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class AggregationServiceImpl implements AggregationService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Value("${rocketmq.topic.push}")
    private String topic;

    private DefaultRedisScript<Long> redisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/aggregate.lua")));
        redisScript.setResultType(Long.class);
    }

    @Override
    public boolean needAggregation(TaskInfo taskInfo) {
        // 简单策略：只有带有 businessId 的消息才聚合
        return StrUtil.isNotBlank(taskInfo.getBusinessId());
    }

    @Override
    public void aggregate(TaskInfo taskInfo) {
        Set<String> receivers = taskInfo.getReceiver();
        long expire = 30; // 聚合窗口 30秒
        long batchId = System.currentTimeMillis() / (expire * 1000); // 生成批次ID

        for (String receiver : receivers) {
            // Key 增加 batchId 后缀，解决并发冲突问题
            // 修改：聚合 Key 去掉 businessId，改用 MessageTemplateId，确保同类消息能聚合
            String key = "agg:data:" + receiver + ":" + taskInfo.getMessageTemplateId() + ":" + batchId;
            String flagKey = "agg:flag:" + receiver + ":" + taskInfo.getMessageTemplateId() + ":" + batchId;
            String counterKey = "agg:count:" + receiver + ":" + taskInfo.getMessageTemplateId() + ":" + batchId;

            try {
                Long result = redisTemplate.execute(redisScript, Arrays.asList(key, flagKey, counterKey),
                        JSON.toJSONString(taskInfo), String.valueOf(expire));

                if (result != null && result == 1) {
                    // 首条消息，发送 Trigger
                    TaskInfo trigger = new TaskInfo();
                    trigger.setBusinessId(taskInfo.getBusinessId());
                    trigger.setMsgType(MessageType.AGGREGATION.getCode()); // 设置为聚合类型
                    trigger.setReceiver(Collections.singleton(receiver)); // Trigger 针对单个接收者
                    
                    // 将原始 MsgType 和 BatchId 放入 params，以便后续恢复
                    Map<String, String> params = new HashMap<>();
                    params.put("originalMsgType", String.valueOf(taskInfo.getMsgType()));
                    params.put("originalTemplateId", String.valueOf(taskInfo.getMessageTemplateId())); // 记录 TemplateId
                    params.put("batchId", String.valueOf(batchId)); // 传递批次ID
                    trigger.setParams(params);

                    // 发送延迟消息 (Level 4 = 30s)
                    try {
                        rocketMQTemplate.syncSend(topic, MessageBuilder.withPayload(JSON.toJSONString(trigger)).build(), 2000, 4);
                        log.info("发送聚合Trigger消息: receiver={}, bizId={}, batchId={}", receiver, taskInfo.getBusinessId(), batchId);
                    } catch (Exception e) {
                        // MQ 发送失败，必须回滚 Redis Flag，否则这批消息将永远无法被触发
                        log.error("发送聚合Trigger失败，回滚Flag: {}", flagKey, e);
                        redisTemplate.delete(flagKey);
                    }
                }
            } catch (Exception e) {
                log.error("聚合处理异常", e);
            }
        }
    }

    @Override
    public TaskInfo getAggregatedData(TaskInfo triggerTaskInfo) {
        // 1. 解析 Trigger 信息
        // String businessId = triggerTaskInfo.getBusinessId(); // 聚合不再依赖 businessId
        String receiver = triggerTaskInfo.getReceiver().iterator().next(); // 获取接收者
        String originalTemplateId = triggerTaskInfo.getParams().get("originalTemplateId");
        String batchId = triggerTaskInfo.getParams().get("batchId"); // 获取批次ID

        // Key 增加 batchId 后缀
        String key = "agg:data:" + receiver + ":" + originalTemplateId + ":" + batchId;
        String counterKey = "agg:count:" + receiver + ":" + originalTemplateId + ":" + batchId;

        // 2. 从 Redis 获取数据 (限制最大条数，防止 OOM)
        // 真实业务场景优化：只取前 100 条，避免 20万条数据一次性拉取把内存撑爆
        List<String> list = redisTemplate.opsForList().range(key, 0, 99);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        
        // 3. 暂时不删除 Key (改为 ACK 机制)
        // redisTemplate.delete(key);
        // redisTemplate.delete("agg:flag:" + receiver + ":" + originalMsgType + ":" + businessId);

        // 4. 聚合逻辑 (合并内容)
        TaskInfo firstTask = JSON.parseObject(list.get(0), TaskInfo.class);
        StringBuilder contentBuilder = new StringBuilder();
        
        // 抖音/小红书模式：只展示前几条详情，后面用数字概括
        // 例如："张三、李四 等 20 人点赞了您的作品"
        int showCount = Math.min(list.size(), 3); // 只展示前3个人名
        for (int i = 0; i < showCount; i++) {
            TaskInfo task = JSON.parseObject(list.get(i), TaskInfo.class);
            // 假设 content 里存的是用户名为
            if (i > 0) contentBuilder.append(", ");
            contentBuilder.append(task.getContent());
        }
        
        // 获取真实总数 (从 Counter 获取)
        String countStr = redisTemplate.opsForValue().get(counterKey);
        long totalCount = countStr == null ? 0 : Long.parseLong(countStr);
        
        if (totalCount > showCount) {
            contentBuilder.append(" and ").append(totalCount).append(" others");
        }
        
        // 更新内容
        firstTask.setContent(contentBuilder.toString());
        
        return firstTask;
    }

    @Override
    public void clear(TaskInfo triggerTaskInfo) {
        String receiver = triggerTaskInfo.getReceiver().iterator().next();
        String originalTemplateId = triggerTaskInfo.getParams().get("originalTemplateId");
        String batchId = triggerTaskInfo.getParams().get("batchId"); // 获取批次ID

        String key = "agg:data:" + receiver + ":" + originalTemplateId + ":" + batchId;
        String flagKey = "agg:flag:" + receiver + ":" + originalTemplateId + ":" + batchId;
        String counterKey = "agg:count:" + receiver + ":" + originalTemplateId + ":" + batchId;

        redisTemplate.delete(Arrays.asList(key, flagKey, counterKey));
    }
}

