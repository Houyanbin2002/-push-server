package com.ethan.push.domain.pipeline.impl;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 前置校验 Action
 * 1. 参数完整性校验
 * (全局限流已移除，交由 MQ 削峰填谷)
 */
@Slf4j
@Service
public class PreCheckAction implements BusinessProcess {

    @Override
    public void process(ProcessContext context) {
        TaskInfo taskInfo = context.getTaskInfo();
        log.info("Pipeline: PreCheckAction start. businessId:{}", taskInfo.getBusinessId());

        // 1. 基础参数校验
        if (taskInfo.getMessageTemplateId() == null) {
            context.setNeedBreak(true);
            context.setResponse(BasicResult.fail("模板ID不能为空"));
            log.warn("PreCheck failed: templateId is null");
            return;
        }
        
        // 2. 全局限流已移除
        // 采用 MQ 异步架构，利用 MQ 的堆积能力应对突发流量，
        // 避免在入口处误杀请求，提高系统吞吐量上限。
    }
}

