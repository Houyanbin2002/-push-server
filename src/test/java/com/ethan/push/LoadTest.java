package com.ethan.push;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.ethan.push.interfaces.dto.MessageParam;
import com.ethan.push.interfaces.dto.SendRequest;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高并发压测脚本
 * 模拟多线程并发调用 /send 接口
 */
public class LoadTest {

    // 压测配置
    private static final int THREAD_COUNT = 50; // 并发线程数 (模拟50个人同时点)
    private static final int TOTAL_REQUESTS = 1000; // 总请求数
    private static final String URL = "http://127.0.0.1:8081/send";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 开始压测 ==========");
        System.out.println("并发线程数: " + THREAD_COUNT);
        System.out.println("总请求数: " + TOTAL_REQUESTS);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            executorService.execute(() -> {
                try {
                    sendRequest();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("请求失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有请求完成
        latch.await();
        long endTime = System.currentTimeMillis();
        long costTime = endTime - startTime;

        System.out.println("========== 压测结束 ==========");
        System.out.println("耗时: " + costTime + "ms");
        System.out.println("成功数: " + successCount.get());
        System.out.println("失败数: " + failCount.get());
        System.out.println("QPS: " + (TOTAL_REQUESTS * 1000.0 / costTime));
        
        executorService.shutdown();
    }

    private static void sendRequest() {
        // 1. 构造请求参数
        // 模拟 5 个不同的接收者，测试聚合效果
        String receiver = "1380013800" + RandomUtil.randomInt(0, 5); 
        
        MessageParam param = MessageParam.builder()
                .receiver(receiver)
                .variables(Collections.singletonMap("name", "用户" + RandomUtil.randomString(4)))
                .build();

        SendRequest request = SendRequest.builder()
                .code("send")
                .messageTemplateId(200L) // 使用真实存在的模板ID (验证码)
                .messageParam(Collections.singletonList(param))
                .build();

        // 2. 发送 HTTP 请求
        String jsonBody = JSON.toJSONString(request);
        try (HttpResponse response = HttpRequest.post(URL)
                .body(jsonBody)
                .timeout(2000) // 2秒超时
                .execute()) {
            
            if (response.getStatus() != 200) {
                throw new RuntimeException("HTTP Status: " + response.getStatus());
            }
        }
    }
}
