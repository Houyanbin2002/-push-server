package com.ethan.push.interfaces.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RestController
public class TestController {

    /**
     * 受害者接口：模拟一个简单的并行任务
     * 正常情况下，这个接口应该在 10ms 内返回。
     * 如果 CommonPool 被占满，这个接口会卡死或超时。
     */
    @GetMapping("/test/victim")
    public String victim() {
        long start = System.currentTimeMillis();
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        
        // 试图使用公共池进行简单的打印
        try {
            numbers.parallelStream().forEach(n -> {
                // 简单操作，不应该耗时
                String s = "Number: " + n;
            });
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

        long end = System.currentTimeMillis();
        return "我依然活着! 耗时: " + (end - start) + "ms | 当前公共池活跃线程数: " + ForkJoinPool.commonPool().getActiveThreadCount();
    }
}
