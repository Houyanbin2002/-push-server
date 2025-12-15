package com.ethan.push.domain.pipeline.impl;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessContext;
import com.ethan.push.domain.service.AggregationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 聚合 Action
 * 负责将消息存入 Redis 并发送延迟 Trigger，从而中断当前发送流程
 */
@Slf4j
@Service
public class AggregationAction implements BusinessProcess {

    @Autowired
    private AggregationService aggregationService;

    @Override
    public void process(ProcessContext context) {
        TaskInfo taskInfo = context.getTaskInfo();
        log.info("Pipeline: AggregationAction start. businessId:{}", taskInfo.getBusinessId());
        
        if (aggregationService.needAggregation(taskInfo)) {
            try {
                aggregationService.aggregate(taskInfo);
                // 聚合后，当前流程中断 (等待 Trigger 触发后续流程)
                context.setNeedBreak(true);
                context.setResponse(BasicResult.success()); // 视为成功
                log.info("Aggregation processed. Flow break. businessId:{}", taskInfo.getBusinessId());
            } catch (Exception e) {
                context.setNeedBreak(true);
                context.setResponse(BasicResult.fail("聚合处理失败"));
                log.error("Aggregation failed", e);
            }
        }
    }
}

