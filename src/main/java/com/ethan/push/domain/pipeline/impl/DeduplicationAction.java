package com.ethan.push.domain.pipeline.impl;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessContext;
import com.ethan.push.domain.service.DeduplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 去重/限流 Action
 */
@Slf4j
@Service
public class DeduplicationAction implements BusinessProcess {

    @Autowired
    private DeduplicationService deduplicationService;

    @Override
    public void process(ProcessContext context) {
        TaskInfo taskInfo = context.getTaskInfo();
        log.info("Pipeline: DeduplicationAction start. businessId:{}", taskInfo.getBusinessId());
        try {
            deduplicationService.deduplication(taskInfo);
        } catch (Exception e) {
            // 如果去重服务抛出异常（例如触发限流），则中断流程
            context.setNeedBreak(true);
            // context.setResponse(BasicResult.fail("触发限流策略"));
            log.warn("Deduplication triggered. businessId:{}", taskInfo.getBusinessId());
        }
    }
}

