package com.ethan.push.domain.service;

import com.ethan.push.domain.entity.MessageTemplate;

/**
 * 消息模板服务接口
 */
public interface MessageTemplateService {

    /**
     * 查询消息模板 (支持多级缓存)
     * @param id 模板ID
     * @return
     */
    MessageTemplate queryById(Long id);
}

