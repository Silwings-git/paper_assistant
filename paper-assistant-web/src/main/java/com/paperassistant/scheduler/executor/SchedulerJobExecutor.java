package com.paperassistant.scheduler.executor;

/**
 * 定时任务业务执行器接口
 */
public interface SchedulerJobExecutor {

    /**
     * 执行任务
     * @return 执行结果描述
     */
    String execute();
}
