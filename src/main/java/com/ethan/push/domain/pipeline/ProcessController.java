package com.ethan.push.domain.pipeline;

import com.ethan.push.interfaces.common.BasicResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 流程控制器
 * 负责串联 Action 节点
 */
@Slf4j
@Component
@Data
public class ProcessController {

    /**
     * 模板映射
     * Key: 业务代码 (如 "send")
     * Value: 该业务对应的 Action 链
     */
    private Map<String, List<BusinessProcess>> templateConfig = null;

    /**
     * 执行责任链
     * @param context 上下文
     * @return 执行结果
     */
    public BasicResult process(ProcessContext context) {
        // 1. 前置检查
        if (templateConfig == null || !templateConfig.containsKey(context.getCode())) {
            return BasicResult.fail("Pipeline config not found");
        }

        // 2. 获取对应的 Action 链
        List<BusinessProcess> processList = templateConfig.get(context.getCode());

        // 3. 渚濇鎵ц
        for (BusinessProcess process : processList) {
            process.process(context);
            if (context.isNeedBreak()) {
                return context.getResponse();
            }
        }

        return context.getResponse();
    }
}

