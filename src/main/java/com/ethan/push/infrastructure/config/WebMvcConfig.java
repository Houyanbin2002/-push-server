package com.ethan.push.infrastructure.config;

import com.ethan.push.infrastructure.utils.TraceIdUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Web 閰嶇疆绫?
 * 娉ㄥ唽 TraceId 鎷︽埅鍣?
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * TraceId 拦截器
     * 负责在 HTTP 请求入口处生成或透传 TraceId
     */
    static class TraceIdInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 1. 尝试从 Header 中获取 TraceId (用于微服务之间透传)
            String traceId = request.getHeader(TraceIdUtils.TRACE_ID);
            
            // 2. 如果 Header 里没有，则生成一个新的
            TraceIdUtils.setTraceId(traceId);
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            // 3. 璇锋眰缁撴潫锛屾竻鐞?MDC锛岄槻姝㈠唴瀛樻硠婕?
            TraceIdUtils.removeTraceId();
        }
    }
}

