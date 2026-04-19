package com.paperassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket 广播服务，向 /topic/task/{taskId} 推送进度
 */
@Service
public class WebSocketBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcastService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketBroadcastService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 推送任务进度
     */
    public void broadcastProgress(String taskId, String stage, int progress, String message) {
        try {
            // Determine status: if progress >= 100, derive terminal status from stage name
            String status = progress >= 100 && ("ANALYZED".equals(stage) || "SEARCHED".equals(stage)) ? stage : null;

            Map<String, Object> payload = Map.of(
                    "taskId", taskId,
                    "stage", stage,
                    "status", status != null ? status : stage,
                    "progress", progress,
                    "message", message
            );
            messagingTemplate.convertAndSend("/topic/task/" + taskId, payload);
            log.debug("Broadcast progress: taskId={}, stage={}, progress={}", taskId, stage, progress);
        } catch (Exception e) {
            log.warn("Failed to broadcast progress for taskId={}", taskId, e);
        }
    }

    /**
     * 推送任务完成
     */
    public void broadcastComplete(String taskId, String stage, Object data) {
        try {
            messagingTemplate.convertAndSend("/topic/task/" + taskId,
                    Map.of("taskId", taskId, "stage", stage, "status", stage, "progress", 100, "data", data));
            log.debug("Broadcast complete: taskId={}, stage={}", taskId, stage);
        } catch (Exception e) {
            log.warn("Failed to broadcast complete for taskId={}", taskId, e);
        }
    }

    /**
     * 推送任务失败
     */
    public void broadcastError(String taskId, String errorMessage) {
        try {
            messagingTemplate.convertAndSend("/topic/task/" + taskId,
                    Map.of("taskId", taskId, "stage", "FAILED", "status", "FAILED", "progress", 0, "message", errorMessage));
            log.debug("Broadcast error: taskId={}", taskId);
        } catch (Exception e) {
            log.warn("Failed to broadcast error for taskId={}", taskId, e);
        }
    }
}
