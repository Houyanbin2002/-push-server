package com.ethan.push;

import com.ethan.push.interfaces.dto.MessageParam;
import com.ethan.push.domain.entity.MessageTemplate;
import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.application.service.SendService;
import com.ethan.push.infrastructure.persistence.mapper.MessageTemplateMapper;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
    RocketMQAutoConfiguration.class, 
    DataSourceAutoConfiguration.class,
    com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
public class PipelineTest {

    @Autowired
    private SendService sendService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;
    
    @MockBean
    private ListOperations<String, String> listOperations;

    @MockBean
    private org.apache.rocketmq.spring.core.RocketMQTemplate rocketMQTemplate;

    @MockBean
    private MessageTemplateMapper messageTemplateMapper;

    @Test
    public void testSendFlow() {
        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // 假设没有去重记录

        // Mock DB
        MessageTemplate mockTemplate = new MessageTemplate();
        mockTemplate.setId(1L);
        mockTemplate.setName("Test Template");
        mockTemplate.setMsgContent("Hello, ${name}!");
        mockTemplate.setMsgStatus(10); // 假设10是有效状态
        mockTemplate.setIsDeleted(0);
        mockTemplate.setSendChannel(10); // Email
        mockTemplate.setMsgType(10); // Notice
        
        when(messageTemplateMapper.selectById(anyLong())).thenReturn(mockTemplate);

        // 构造请求
        MessageParam param = MessageParam.builder()
                .receiver("13800138000")
                .variables(Collections.singletonMap("name", "TestUser"))
                .build();

        SendRequest request = SendRequest.builder()
                .code("send")
                .messageTemplateId(1L)
                .messageParam(Collections.singletonList(param))
                .build();

        // 执行发送
        BasicResult result = sendService.send(request);

        // 验证结果
        System.out.println("Test Result: " + result.getStatus() + " - " + result.getMsg());
        assert result.getStatus().equals("success");
    }
}
