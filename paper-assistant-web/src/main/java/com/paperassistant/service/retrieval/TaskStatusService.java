package com.paperassistant.service.retrieval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paperassistant.dto.TaskStatusDTO;
import com.paperassistant.entity.TaskStatusEntity;
import com.paperassistant.exception.BusinessException;
import com.paperassistant.exception.ErrorCode;
import com.paperassistant.mapper.TaskStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 任务状态持久化服务
 */
@Service
public class TaskStatusService {

    private static final Logger log = LoggerFactory.getLogger(TaskStatusService.class);

    private final TaskStatusMapper taskStatusMapper;

    public TaskStatusService(TaskStatusMapper taskStatusMapper) {
        this.taskStatusMapper = taskStatusMapper;
    }

    /**
     * 创建任务状态记录
     */
    public TaskStatusDTO createTask(String taskId, String taskType, String projectId) {
        TaskStatusEntity entity = new TaskStatusEntity();
        entity.setTaskId(taskId);
        entity.setTaskType(taskType);
        entity.setProjectId(projectId);
        entity.setStatus("PENDING");
        entity.setProgress(0);
        entity.setStage("等待开始");
        taskStatusMapper.insert(entity);
        log.info("Task created: taskId={}, type={}", taskId, taskType);
        return toDTO(entity);
    }

    /**
     * 更新任务状态
     */
    public void updateTask(String taskId, String status, Integer progress, String stage, String message) {
        TaskStatusEntity entity = getByTaskId(taskId);
        entity.setStatus(status);
        if (progress != null) entity.setProgress(progress);
        if (stage != null) entity.setStage(stage);
        if (message != null) entity.setMessage(message);
        taskStatusMapper.updateById(entity);
    }

    /**
     * 更新任务结果数据
     */
    public void updateTaskResult(String taskId, String resultData) {
        TaskStatusEntity entity = getByTaskId(taskId);
        entity.setResultData(resultData);
        taskStatusMapper.updateById(entity);
    }

    /**
     * 查询任务状态
     */
    public TaskStatusDTO getTaskStatus(String taskId) {
        TaskStatusEntity entity = getByTaskId(taskId);
        return toDTO(entity);
    }

    /**
     * 查询项目最新的进行中任务
     */
    public TaskStatusDTO getLatestActiveTask(String projectId, String taskType) {
        LambdaQueryWrapper<TaskStatusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskStatusEntity::getProjectId, projectId);
        wrapper.eq(TaskStatusEntity::getTaskType, taskType);
        wrapper.notIn(TaskStatusEntity::getStatus, "SEARCHED", "ANALYZED", "FAILED");
        wrapper.orderByDesc(TaskStatusEntity::getCreateTime);
        wrapper.last("LIMIT 1");
        TaskStatusEntity entity = taskStatusMapper.selectOne(wrapper);
        if (entity == null) {
            return null;
        }
        return toDTO(entity);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        TaskStatusEntity entity = getByTaskId(taskId);
        String currentStatus = entity.getStatus();
        // 只有非终态任务可以取消
        if ("SEARCHED".equals(currentStatus) || "ANALYZED".equals(currentStatus)
                || "FAILED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            log.warn("Cannot cancel task in terminal state: taskId={}, status={}", taskId, currentStatus);
            return false;
        }
        entity.setStatus("CANCELLED");
        entity.setMessage("任务已取消");
        taskStatusMapper.updateById(entity);
        log.info("Task cancelled: taskId={}", taskId);
        return true;
    }

    /**
     * 检查任务是否已取消
     */
    public boolean isCancelled(String taskId) {
        try {
            TaskStatusEntity entity = getByTaskId(taskId);
            return "CANCELLED".equals(entity.getStatus());
        } catch (BusinessException e) {
            // 任务不存在，视为已取消
            return true;
        }
    }

    /**
     * 清理过期任务 (超过 7 天)
     */
    public int cleanupOldTasks() {
        LambdaQueryWrapper<TaskStatusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(TaskStatusEntity::getCreateTime, java.time.LocalDateTime.now().minusDays(7));
        return taskStatusMapper.delete(wrapper);
    }

    private TaskStatusEntity getByTaskId(String taskId) {
        LambdaQueryWrapper<TaskStatusEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskStatusEntity::getTaskId, taskId);
        TaskStatusEntity entity = taskStatusMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在: " + taskId);
        }
        return entity;
    }

    private TaskStatusDTO toDTO(TaskStatusEntity entity) {
        TaskStatusDTO dto = new TaskStatusDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setTaskType(entity.getTaskType());
        dto.setProjectId(entity.getProjectId());
        dto.setStatus(entity.getStatus());
        dto.setProgress(entity.getProgress());
        dto.setStage(entity.getStage());
        dto.setMessage(entity.getMessage());
        dto.setResultData(entity.getResultData());
        return dto;
    }
}
