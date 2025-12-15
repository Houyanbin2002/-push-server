package com.ethan.push.application.service;

import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.interfaces.common.BasicResult;

public interface SendService {

    /**
     * 发送消息
     * @param sendRequest
     * @return
     */
    BasicResult send(SendRequest sendRequest);
}

