package com.ethan.push.domain.service;

import com.ethan.push.domain.model.TaskInfo;

/**
 * 去重服务接口
 */
public interface DeduplicationService {

    /**
     * 去重/限流
     * @param taskInfo
     */
    void deduplication(TaskInfo taskInfo);
}

