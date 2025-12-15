package com.ethan.push.domain.pipeline;

/**
 * 业务执行接口
 * 所有的 Action 都要实现这个接口
 */
public interface BusinessProcess {

    /**
     * 执行业务逻辑
     * @param context 上下文
     */
    void process(ProcessContext context);
}

