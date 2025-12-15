package com.ethan.push.domain.pipeline.impl;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.domain.enums.ChannelType;
import com.ethan.push.domain.enums.MessageType;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.entity.MessageTemplate;
import com.ethan.push.domain.pipeline.BusinessProcess;
import com.ethan.push.domain.pipeline.ProcessContext;
import com.ethan.push.domain.service.MessageTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 拼装参数 Action
 * 1. 查询消息模板
 * 2. 替换占位符
 * 3. 组装 TaskInfo
 */
@Slf4j
@Service
public class AssembleAction implements BusinessProcess {

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Override
    public void process(ProcessContext context) {
        TaskInfo taskInfo = context.getTaskInfo();
        log.info("Pipeline: AssembleAction start. businessId:{}", taskInfo.getBusinessId());
        try {
            // 1. 查询模板 (多级缓存)
            MessageTemplate messageTemplate = messageTemplateService.queryById(taskInfo.getMessageTemplateId());
            if (messageTemplate == null || messageTemplate.getIsDeleted() == 1) {
                context.setNeedBreak(true);
                context.setResponse(BasicResult.fail("Template not found or deleted"));
                log.warn("Assemble failed: template not found. id:{}", taskInfo.getMessageTemplateId());
                return;
            }

            // 2. 组装参数
            String content = messageTemplate.getMsgContent();
            Map<String, String> params = taskInfo.getParams();
            
            // 简单替换占位符 {$var} -> value
            if (StrUtil.isNotBlank(content) && params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    content = content.replace("{$" + entry.getKey() + "}", entry.getValue());
                }
            }

            // 3. 补全 TaskInfo
            taskInfo.setContent(content);
            taskInfo.setSendChannel(messageTemplate.getSendChannel());
            taskInfo.setMsgType(messageTemplate.getMsgType());
            taskInfo.setShieldType(messageTemplate.getShieldType());
            
            // 4. 业务ID生成 (如果没有)
            if (StrUtil.isBlank(taskInfo.getBusinessId())) {
                taskInfo.setBusinessId(String.valueOf(taskInfo.getMessageTemplateId()) + System.currentTimeMillis());
            }

        } catch (Exception e) {
            context.setNeedBreak(true);
            context.setResponse(BasicResult.fail("组装参数失败: " + e.getMessage()));
            log.error("assemble task fail! taskInfo:{}", JSON.toJSONString(taskInfo), e);
        }
    }
}

