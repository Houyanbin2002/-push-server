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

        // 3. 提交 Pipeline 处理
        // 修复：移除 parallelStream，避免占用 JVM 公共线程池导致雪崩
        // 这里的处理逻辑很快（只是组装 Context），不需要并行
        for (TaskInfo taskInfo : taskInfoList) {
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
        }

        return BasicResult.success();
    }
}

