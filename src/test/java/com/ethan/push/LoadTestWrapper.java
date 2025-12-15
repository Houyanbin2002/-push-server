package com.ethan.push;

import org.junit.jupiter.api.Test;

public class LoadTestWrapper {

    @Test
    public void runLoadTest() throws InterruptedException {
        // 只有当服务启动时才运行压测
        // 这里为了演示，直接调用 main
        // 实际运行前请确保 8081 端口已启动
        try {
            LoadTest.main(new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
