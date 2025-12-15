package com.ethan.push.infrastructure.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler 路由分发器
 */
@Component
public class HandlerHolder {

    private Map<Integer, Handler> handlerMap = new HashMap<>();

    public void putHandler(Integer channelCode, Handler handler) {
        handlerMap.put(channelCode, handler);
    }

    public Handler route(Integer channelCode) {
        return handlerMap.get(channelCode);
    }
}


