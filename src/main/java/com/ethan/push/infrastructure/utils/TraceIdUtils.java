package com.ethan.push.infrastructure.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

public class TraceIdUtils {
    public static final String TRACE_ID = "traceId";
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    public static String getTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            TRACE_ID_HOLDER.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }

    public static void removeTraceId() {
        TRACE_ID_HOLDER.remove();
    }
}
