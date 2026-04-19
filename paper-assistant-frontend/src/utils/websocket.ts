import { Client } from '@stomp/stompjs'
// @ts-ignore sockjs-client lacks proper TS declarations
import SockJS from 'sockjs-client/dist/sockjs.min.js'

interface TaskMessage {
  taskId: string;
  stage: string;
  progress: number;
  message: string;
  status: string;
  data?: unknown;
}

interface TaskCallbacks {
  onProgress?: (msg: TaskMessage) => void;
  onComplete?: (msg: TaskMessage) => void;
  onError?: (message: string) => void;
}

interface PollingCallback {
  taskId: string;
  callback: (msg: TaskMessage) => void;
}

export class StompManager {
  private client: Client | null = null;
  private connected = false;
  private reconnectAttempts = 0;
  private maxReconnects = 5;
  private subscriptions: Map<string, (msg: TaskMessage) => void> = new Map();
  private taskSubscriptions: Map<string, TaskCallbacks> = new Map();

  // 降级轮询
  private fallbackMode = false;
  private pollingCallbacks: Map<string, PollingCallback[]> = new Map();
  private pollingTimers: Map<string, ReturnType<typeof setInterval>> = new Map();

  connect() {
    this.client = new Client({
      webSocketFactory: () => new (SockJS as any)('/ws'),
      reconnectDelay: 3000,
      heartbeatIncoming: 30000,
      heartbeatOutgoing: 30000,
      onConnect: () => {
        this.connected = true;
        this.reconnectAttempts = 0;
        console.log('WebSocket connected');
        // Re-subscribe after reconnect
        this.subscriptions.forEach((callback, taskId) => this.subscribe(taskId, callback));
      },
      onDisconnect: () => {
        this.connected = false;
        console.log('WebSocket disconnected');
        this.handleReconnectFailure();
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        this.handleReconnectFailure();
      },
    });

    this.client.activate();
  }

  /**
   * 处理重连失败，超过 5 次后切换到 HTTP 轮询
   */
  private handleReconnectFailure() {
    this.reconnectAttempts++;
    if (this.reconnectAttempts >= this.maxReconnects && !this.fallbackMode) {
      console.warn(
        `WebSocket reconnected ${this.reconnectAttempts} times, falling back to HTTP polling`
      );
      this.fallbackMode = true;
      // 切换所有活跃订阅到轮询模式
      this.switchToPolling();
    }
  }

  /**
   * 切换到 HTTP 轮询降级模式
   */
  private switchToPolling() {
    this.pollingCallbacks.forEach((_callbacks, taskId) => {
      this.startPolling(taskId);
    });
  }

  /**
   * 启动单个任务的 HTTP 轮询
   */
  private startPolling(taskId: string) {
    if (this.pollingTimers.has(taskId)) return;

    const timer = setInterval(async () => {
      try {
        const res = await fetch(`/api/v1/papers/tasks/${taskId}`);
        if (!res.ok) return;
        const data = await res.json();
        const msg = data.data as TaskMessage;
        const callbacks = this.pollingCallbacks.get(taskId) || [];
        callbacks.forEach(cb => cb.callback(msg));
        // 任务完成后停止轮询
        if (msg.status === 'ANALYZED' || msg.status === 'SEARCHED' || msg.status === 'FAILED') {
          clearInterval(timer);
          this.pollingTimers.delete(taskId);
        }
      } catch (e) {
        console.error('HTTP polling failed for task', taskId, e);
      }
    }, 3000);

    this.pollingTimers.set(taskId, timer);
  }

  disconnect() {
    this.client?.deactivate();
    this.connected = false;
    // 清理轮询定时器
    this.pollingTimers.forEach(timer => clearInterval(timer));
    this.pollingTimers.clear();
    this.pollingCallbacks.clear();
  }

  subscribe(taskId: string, callback: (msg: TaskMessage) => void) {
    this.subscriptions.set(taskId, callback);

    if (this.fallbackMode || (this.client && this.connected)) {
      if (this.fallbackMode) {
        // 降级模式：加入轮询
        let callbacks = this.pollingCallbacks.get(taskId);
        if (!callbacks) {
          callbacks = [];
          this.pollingCallbacks.set(taskId, callbacks);
          this.startPolling(taskId);
        }
        callbacks.push({ taskId, callback });
      } else {
        this.client!.subscribe(`/topic/task/${taskId}`, (message) => {
          callback(JSON.parse(message.body));
        });
      }
    }
  }

  unsubscribe(taskId: string) {
    this.subscriptions.delete(taskId);
    this.pollingCallbacks.delete(taskId);
    const timer = this.pollingTimers.get(taskId);
    if (timer) {
      clearInterval(timer);
      this.pollingTimers.delete(taskId);
    }
  }

  /**
   * 高级任务订阅：支持 onProgress/onComplete/onError 回调
   */
  subscribeTask(taskId: string, callbacks: TaskCallbacks) {
    this.taskSubscriptions.set(taskId, callbacks)

    // 注册底层 subscribe（自动处理 fallback）
    this.subscribe(taskId, (msg) => {
      const cbs = this.taskSubscriptions.get(taskId)
      if (!cbs) return

      const terminal = ['ANALYZED', 'SEARCHED', 'FAILED', 'CANCELLED']
      if (terminal.includes(msg.status)) {
        // 先更新进度条状态到终态
        cbs.onProgress?.(msg)
        if (msg.status === 'CANCELLED' || msg.status === 'FAILED') {
          cbs.onError?.(msg.message || '任务已取消')
        } else {
          cbs.onComplete?.(msg)
        }
      } else {
        cbs.onProgress?.(msg)
      }
    })
  }

  /**
   * 取消任务订阅
   */
  unsubscribeTask(taskId: string) {
    this.taskSubscriptions.delete(taskId)
    this.unsubscribe(taskId)
  }

  isConnected(): boolean {
    return this.connected;
  }

  /**
   * 是否处于降级轮询模式
   */
  isFallbackMode(): boolean {
    return this.fallbackMode;
  }
}

export const stompManager = new StompManager();
