package com.ethan.push;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ethan.push.interfaces.dto.MessageParam;
import com.ethan.push.interfaces.dto.SendRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

/**
 * 功能性测试脚本
 * 覆盖：验证码、营销、通知、去重、屏蔽
 */
public class FunctionalTest {

    private static final String URL = "http://127.0.0.1:8081/send";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/push_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1234";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 开始功能测试 ==========");
        
        // 0. 初始化数据
        initDb();

        // 1. 测试验证码 (高优，不应被去重)
        testVerificationCode();

        // 2. 测试营销消息 (低优，应触发去重)
        testMarketingDeduplication();

        // 3. 测试夜间屏蔽 (假设当前是夜间，或者模板配置了屏蔽)
        testShielding();

        System.out.println("========== 功能测试结束 ==========");
        System.exit(0);
    }

    private static void initDb() {
        System.out.println("[0] 初始化测试数据...");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            
            // 插入验证码模板 (ID=200)
            String sql2 = "INSERT IGNORE INTO `message_template` " +
                    "(`id`, `name`, `audit_status`, `flow_id`, `msg_status`, `id_type`, `send_channel`, `template_type`, `msg_type`, `shield_type`, `msg_content`, `send_account`, `creator`, `updator`, `auditor`, `team`, `proposer`, `is_deleted`, `created`, `updated`) " +
                    "VALUES (200, '验证码模板', 20, 'flow_code', 30, 30, 30, 40, 30, 10, '{\"content\":\"您的验证码是{$code}\"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000)";
            stmt.executeUpdate(sql2);

            // 插入营销模板 (ID=300)
            String sql3 = "INSERT IGNORE INTO `message_template` " +
                    "(`id`, `name`, `audit_status`, `flow_id`, `msg_status`, `id_type`, `send_channel`, `template_type`, `msg_type`, `shield_type`, `msg_content`, `send_account`, `creator`, `updator`, `auditor`, `team`, `proposer`, `is_deleted`, `created`, `updated`) " +
                    "VALUES (300, '营销模板', 20, 'flow_market', 30, 30, 30, 30, 20, 10, '{\"content\":\"双11大促，全场5折！\"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000)";
            stmt.executeUpdate(sql3);

            // 插入夜间屏蔽模板 (ID=400)
            String sql4 = "INSERT IGNORE INTO `message_template` " +
                    "(`id`, `name`, `audit_status`, `flow_id`, `msg_status`, `id_type`, `send_channel`, `template_type`, `msg_type`, `shield_type`, `msg_content`, `send_account`, `creator`, `updator`, `auditor`, `team`, `proposer`, `is_deleted`, `created`, `updated`) " +
                    "VALUES (400, '夜间屏蔽模板', 20, 'flow_shield', 30, 30, 30, 10, 10, 20, '{\"content\":\"这是一条夜间屏蔽消息\"}', 66, 'Ethan', 'Ethan', 'Ethan', 'EthanTeam', 'Ethan', 0, 1700000000, 1700000000)";
            stmt.executeUpdate(sql4);
            
            // 验证数据是否存在
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM message_template WHERE id IN (200, 300, 400)");
            if (rs.next()) {
                System.out.println("数据库中模板数量 (ID 200,300,400): " + rs.getInt(1));
            }
            
            System.out.println("数据库初始化完成");
            
            // 不再需要长时间等待，因为 ID 是新的
            System.out.println("使用新ID绕过缓存，无需等待...");
            
        } catch (Exception e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
        }
    }

    private static void testVerificationCode() {
        System.out.println("\n[1] 测试验证码发送 (TemplateId=200)...");
        // 发送 5 次，应该全部成功
        int success = 0;
        for (int i = 0; i < 5; i++) {
            if (send(200L, "13800000001", "1234")) success++;
        }
        System.out.println("期望成功: 5, 实际成功: " + success);
    }

    private static void testMarketingDeduplication() {
        System.out.println("\n[2] 测试营销消息去重 (TemplateId=300)...");
        System.out.println("规则: 5分钟内同用户限制3条");
        
        String receiver = "13800000002";
        int success = 0;
        int fail = 0;
        
        // 尝试发送 10 次
        for (int i = 0; i < 10; i++) {
            boolean result = send(300L, receiver, "SALE");
            if (result) {
                success++;
            } else {
                fail++;
            }
        }
        System.out.println("发送 10 次 -> 成功: " + success + ", 失败(被去重): " + fail);
        if (success > 0 && success <= 3 && fail >= 7) {
            System.out.println("✅ 去重功能正常 (成功数 1~3)");
        } else {
            System.err.println("❌ 去重功能异常 (成功数: " + success + ")");
        }
    }

    private static void testShielding() {
        System.out.println("\n[3] 测试夜间屏蔽 (TemplateId=400)...");
        // 假设 ShieldAction 尚未实现或未配置时间段，这里主要测试流程是否通畅
        // 如果 ShieldAction 实现了，这里应该返回失败或特定状态
        boolean result = send(400L, "13800000003", "SHIELD");
        System.out.println("发送结果: " + (result ? "成功" : "失败 (被屏蔽)"));
    }

    private static boolean send(Long templateId, String receiver, String code) {
        MessageParam param = MessageParam.builder()
                .receiver(receiver)
                .variables(Collections.singletonMap("code", code))
                .build();

        SendRequest request = SendRequest.builder()
                .code("send")
                .messageTemplateId(templateId)
                .messageParam(Collections.singletonList(param))
                .build();

        String jsonBody = JSON.toJSONString(request);
        try (HttpResponse response = HttpRequest.post(URL)
                .body(jsonBody)
                .timeout(2000)
                .execute()) {
            
            String body = response.body();
            JSONObject json = JSON.parseObject(body);
            
            // 打印一下响应方便调试
            System.out.println("Resp: " + body);
            
            if (json != null && "success".equals(json.getString("status"))) { // 假设 0 是成功
                 return true;
            }
            // 或者检查 code
             if (json != null && "200".equals(json.getString("code"))) {
                 return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("请求异常: " + e.getMessage());
            return false;
        }
    }
}
