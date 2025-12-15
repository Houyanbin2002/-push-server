package com.ethan.push.domain.pipeline;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.interfaces.common.BasicResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 责任链上下文
 * 存储责任链执行过程中的数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(chain = true)
public class ProcessContext {

    /**
     * 标识责任链的code
     */
    private String code;

    /**
     * 存储核心任务信息
     */
    private TaskInfo taskInfo;

    /**
     * 瀛樺偍璐ｄ换閾句笂涓嬫枃鐨勪腑闂寸粨鏋?
     * 濡傛灉涓柇锛岃繖閲屽瓨鍌ㄤ腑鏂師鍥?
     */
    private BasicResult response;

    /**
     * 是否需要中断责任链
     */
    private boolean needBreak;
}

