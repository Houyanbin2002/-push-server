package com.ethan.push.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ethan.push.domain.entity.MessageTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息模板 Mapper
 */
@Mapper
public interface MessageTemplateMapper extends BaseMapper<MessageTemplate> {
}

