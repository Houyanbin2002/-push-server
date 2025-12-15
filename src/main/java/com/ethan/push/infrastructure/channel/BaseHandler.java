package com.ethan.push.infrastructure.channel;

import com.ethan.push.domain.model.TaskInfo;
import com.ethan.push.infrastructure.utils.FlowControlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;

/**
 * 发送处理器的抽象基类 (模板方法模式)
 * 固化流程：参数校验 -> 流量控制 -> 执行发送
 */
@Slf4j
public abstract class BaseHandler implements Handler {

    @Autowired
    private HandlerHolder handlerHolder;

    @Autowired
    private FlowControlUtils flowControlUtils;

    /**
     * 初始化：将自己注册到 HandlerHolder
     */
    @PostConstruct
    public void init() {
        handlerHolder.putHandler(getChannelCode(), this);
    }

    @Override
    public boolean send(TaskInfo taskInfo) {
        // 1. 参数校验 (留给子类扩展，或在此做通用校验)
        if (taskInfo == null) {
            return false;
        }

        // 2. 渠道流量控制 (保护第三方服务)
        // acquire() 会阻塞当前线程，直到拿到令牌，从而平滑流量
        flowControlUtils.acquire(getChannelCode());

        // 3. 执行真正的发送逻辑 (由子类实现)
        return handler(taskInfo);
    }

    /**
     * 具体的发送逻辑，由子类实现
     */
    public abstract boolean handler(TaskInfo taskInfo);

    /**
     * 获取当前 Handler 对应的渠道 Code
     */
    public abstract Integer getChannelCode();
}

