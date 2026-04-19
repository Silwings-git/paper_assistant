package com.paperassistant.scheduler.job;

import com.paperassistant.scheduler.executor.SchedulerJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 任务状态清理定时任务
 */
@Component
public class TaskStatusCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusCleanupJob.class);

    private final SchedulerJobExecutor taskStatusCleanup;

    public TaskStatusCleanupJob(SchedulerJobExecutor taskStatusCleanup) {
        this.taskStatusCleanup = taskStatusCleanup;
    }

    @Scheduled(cron = "${scheduler.jobs.task-status-cleanup.cron}")
    public void execute() {
        log.info("TaskStatusCleanupJob started");
        try {
            String result = taskStatusCleanup.execute();
            log.info("TaskStatusCleanupJob: {}", result);
        } catch (Exception e) {
            log.error("TaskStatusCleanupJob failed", e);
        }
    }
}
