package com.ethan.push;

import com.ethan.push.interfaces.dto.MessageParam;
import com.ethan.push.interfaces.dto.SendRequest;
import com.ethan.push.interfaces.common.BasicResult;
import com.ethan.push.domain.service.AggregationService;
import com.ethan.push.domain.service.DeduplicationService;
import com.ethan.push.application.service.SendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class PushFlowTest {

    @Autowired
    private SendService sendService;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @MockBean
    private DeduplicationService deduplicationService;

    @MockBean
    private AggregationService aggregationService;

    @Test
    public void testSendFlow() {
        // 1. Prepare Data
        SendRequest request = new SendRequest();
        request.setCode("send");
        request.setMessageTemplateId(1L); // Matches the mock in SendServiceImpl (ID=1)
        
        MessageParam param = new MessageParam();
        param.setReceiver("13800138000");
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "TestUser");
        param.setVariables(vars);
        
        request.setMessageParam(Collections.singletonList(param));

        // 2. Mock Behaviors
        // Deduplication and Aggregation services do nothing (pass through)
        doNothing().when(deduplicationService).deduplication(any());
        // AggregationService.needAggregation returns false by default for mocks, so it might skip aggregate()
        // But AggregationAction calls needAggregation(). If it returns false, it skips.
        // If we want to test the flow reaching MQ, skipping aggregation is fine.

        // 3. Execute
        BasicResult result = sendService.send(request);

        // 4. Verify Result
        assertEquals("200", result.getStatus());
        System.out.println("Test Result: " + result.getMsg());

        // 5. Verify MQ was called
        // SendMqAction calls rocketMQTemplate.asyncSend
        verify(rocketMQTemplate).asyncSend(anyString(), (org.springframework.messaging.Message<?>) any(), any(org.apache.rocketmq.client.producer.SendCallback.class));
    }
}
