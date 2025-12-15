package com.ethan.push.infrastructure.channel;

import com.ethan.push.domain.model.TaskInfo;

/**
 * 发送处理器接口
 */
public interface Handler {

    /**
     * 处理的渠道代码
     */
    Integer getChannelCode();

    /**
     * 执行发送
     */
    boolean send(TaskInfo taskInfo);
}

