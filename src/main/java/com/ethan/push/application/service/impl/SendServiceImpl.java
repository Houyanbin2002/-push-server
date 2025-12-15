package com.ethan.push.application.service.impl;

import cn.hutool.core.util.IdUtil;
import com.ethan.push.interfaces.dto.MessageParam;
import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.pipeline.ProcessContext;
import com.ethan.push.domain.pipeline.ProcessController;
import com.ethan.push.application.service.SendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SendServiceImpl implements SendService {

    @Autowired
    private ProcessController processController;

    @Override
    public BasicResult send(SendRequest sendRequest) {
        // 1. 参数校验
        if (sendRequest == null || sendRequest.getMessageTemplateId() == null || sendRequest.getMessageParam() == null) {
            return BasicResult.fail("参数错误");
        }

        // 2. 组装 TaskInfo (只组装基础信息，模板查询和参数替换交给 Pipeline)
        List<TaskInfo> taskInfoList = new ArrayList<>();
        
        for (MessageParam param : sendRequest.getMessageParam()) {
            TaskInfo taskInfo = TaskInfo.builder()
                    .messageTemplateId(sendRequest.getMessageTemplateId())
                    .businessId(IdUtil.getSnowflake().nextIdStr()) // 使用雪花算法生成唯一ID
                    .receiver(Collections.singleton(param.getReceiver()))
                    .params(param.getVariables()) // 传递变量给 AssembleAction
                    .build();
            
            taskInfoList.add(taskInfo);
        }

        // 3. 骞惰鎻愪氦 Pipeline 澶勭悊
        // 使用 parallelStream 实现简单的并行处理，提高吞吐量
        taskInfoList.parallelStream().forEach(taskInfo -> {
            ProcessContext context = ProcessContext.builder()
                    .code("send")
                    .taskInfo(taskInfo)
                    .needBreak(false)
                    .response(BasicResult.success())
                    .build();
            
            try {
                processController.process(context);
            } catch (Exception e) {
                log.error("Pipeline process failed. businessId:{}", taskInfo.getBusinessId(), e);
            }
        });

        return BasicResult.success();
    }
}

