package com.ethan.push.interfaces.api;

import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.application.service.SendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SendController {

    @Autowired
    private SendService sendService;

    /**
     * 发送消息接口 (Austin 风格)
     */
    @PostMapping("/send")
    public BasicResult send(@RequestBody SendRequest sendRequest) {
        return sendService.send(sendRequest);
    }
}

