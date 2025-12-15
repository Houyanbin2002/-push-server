package com.ethan.push.domain.service;

import com.ethan.push.domain.model.TaskInfo;

/**
 * 聚合服务接口
 */
public interface AggregationService {

    /**
     * 是否需要聚合
     */
    boolean needAggregation(TaskInfo taskInfo);

    /**
     * 执行聚合逻辑 (存入Redis + 发送延迟Trigger)
     */
    void aggregate(TaskInfo taskInfo);

    /**
     * 获取聚合后的数据 (Trigger触发后调用)
     * 注意：此方法不会删除 Redis 数据，需配合 clear 使用
     * @param triggerTaskInfo 触发消息
     * @return 聚合后的 TaskInfo (如果为空说明无数据)
     */
    TaskInfo getAggregatedData(TaskInfo triggerTaskInfo);

    /**
     * 消费成功后清理聚合数据 (ACK)
     * @param triggerTaskInfo 触发消息
     */
    void clear(TaskInfo triggerTaskInfo);
}

