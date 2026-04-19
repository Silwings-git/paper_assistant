#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$PROJECT_ROOT/logs"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()    { echo -e "${CYAN}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[OK]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

# 加载 .env 环境变量
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
    info "已加载 .env 配置"
fi

check_dep() {
  if ! command -v "$1" &>/dev/null; then
    error "未找到 $1，请先安装"
    exit 1
  fi
}

# ========== Docker Compose 操作 ==========
compose() {
  docker compose -f "$COMPOSE_FILE" "$@"
}

start_all() {
  check_dep docker
  check_dep mvn
  check_dep npm
  mkdir -p "$PROJECT_ROOT/docker/data/postgresql" "$PROJECT_ROOT/docker/data/redis"

  # 预构建后端 JAR
  info "编译后端..."
  cd "$PROJECT_ROOT/paper-assistant-web"
  mvn clean package -DskipTests -q
  cd "$PROJECT_ROOT"

  # 预构建前端
  info "构建前端..."
  cd "$PROJECT_ROOT/paper-assistant-frontend"
  npm install
  cd "$PROJECT_ROOT"

  info "启动所有服务..."
  compose up -d --build

  # 等待后端就绪
  info "等待后端就绪..."
  for i in $(seq 1 60); do
    if curl -sf http://localhost:8080/api/v1/projects &>/dev/null; then
      success "后端就绪"
      break
    fi
    if [ "$i" -eq 60 ]; then
      error "后端启动超时 (60s)"
      compose logs backend
      exit 1
    fi
    sleep 1
  done

  # 等待前端就绪
  info "等待前端就绪..."
  for i in $(seq 1 30); do
    if curl -sf http://localhost:5173/ &>/dev/null; then
      success "前端就绪"
      break
    fi
    if [ "$i" -eq 30 ]; then
      error "前端启动超时 (30s)"
      compose logs frontend
      exit 1
    fi
    sleep 1
  done
}

stop_all() {
  info "停止所有服务..."
  compose down
  success "所有服务已停止"
}

restart_all() {
  stop_all
  sleep 2
  start_all
}

show_status() {
  echo ""
  echo "=== 服务状态 ==="
  echo ""
  compose ps
  echo ""

  if curl -sf http://localhost:8080/api/v1/projects &>/dev/null; then
    echo -e "${GREEN}后端${NC} - 运行中 - http://localhost:8080"
  else
    echo -e "${RED}后端${NC} - 未就绪"
  fi

  if curl -sf http://localhost:5173/ &>/dev/null; then
    echo -e "${GREEN}前端${NC} - 运行中 - http://localhost:5173"
  else
    echo -e "${RED}前端${NC} - 未就绪"
  fi
  echo ""
}

show_logs() {
  compose logs -f "${1:-}"
}

# ========== 单组件重启 ==========

VALID_SERVICES="postgres redis backend frontend"

is_valid_service() {
  for s in $VALID_SERVICES; do
    [ "$s" = "$1" ] && return 0
  done
  return 1
}

restart_service() {
  local svc="$1"
  if ! is_valid_service "$svc"; then
    error "无效的服务名: $svc (可选: $VALID_SERVICES)"
    exit 1
  fi

  info "停止 $svc ..."
  compose stop "$svc"
  sleep 1

  # 后端需要重新构建 JAR，前端需要重新构建
  if [ "$svc" = "backend" ]; then
    check_dep mvn
    info "重新编译后端..."
    cd "$PROJECT_ROOT/paper-assistant-web"
    mvn clean package -DskipTests -q
    cd "$PROJECT_ROOT"
    info "构建并启动后端..."
    compose up -d --build "$svc"
  elif [ "$svc" = "frontend" ]; then
    info "重新构建前端..."
    cd "$PROJECT_ROOT/paper-assistant-frontend"
    npm install
    cd "$PROJECT_ROOT"
    info "构建并启动前端..."
    compose up -d --build "$svc"
  else
    info "启动 $svc ..."
    compose up -d "$svc"
  fi

  # 等待就绪
  if [ "$svc" = "backend" ]; then
    info "等待后端就绪..."
    for i in $(seq 1 60); do
      if curl -sf http://localhost:8080/api/v1/projects &>/dev/null; then
        success "后端就绪"
        return
      fi
      sleep 1
    done
    error "后端启动超时 (60s)"
    compose logs backend
    exit 1
  elif [ "$svc" = "frontend" ]; then
    info "等待前端就绪..."
    for i in $(seq 1 30); do
      if curl -sf http://localhost:5173/ &>/dev/null; then
        success "前端就绪"
        return
      fi
      sleep 1
    done
    error "前端启动超时 (30s)"
    compose logs frontend
    exit 1
  fi

  success "$svc 已重启"
}

rebuild_service() {
  local svc="$1"
  if ! is_valid_service "$svc"; then
    error "无效的服务名: $svc (可选: $VALID_SERVICES)"
    exit 1
  fi

  info "重建 $svc 镜像..."
  if [ "$svc" = "backend" ]; then
    check_dep mvn
    cd "$PROJECT_ROOT/paper-assistant-web"
    mvn clean package -DskipTests -q
    cd "$PROJECT_ROOT"
  elif [ "$svc" = "frontend" ]; then
    cd "$PROJECT_ROOT/paper-assistant-frontend"
    npm install
    cd "$PROJECT_ROOT"
  fi

  compose build "$svc"
  success "$svc 镜像重建完成"
}

# ========== 主逻辑 ==========
mkdir -p "$LOG_DIR"

case "${1:-start}" in
  start)
    info "一键启动 Paper Assistant (Docker)"
    start_all
    echo ""
    success "启动完成!"
    echo -e "  后端: ${CYAN}http://localhost:8080${NC}"
    echo -e "  前端: ${CYAN}http://localhost:5173${NC}"
    echo -e "  API文档: ${CYAN}http://localhost:8080/swagger-ui.html${NC}"
    echo ""
    echo "停止服务: $0 stop"
    echo "查看状态: $0 status"
    echo "重启单组件: $0 restart <service>"
    ;;
  stop)
    stop_all
    ;;
  restart)
    if [ -n "${2:-}" ] && is_valid_service "$2"; then
      info "重启 $2"
      restart_service "$2"
    else
      restart_all
    fi
    ;;
  rebuild)
    if [ -n "${2:-}" ] && is_valid_service "$2"; then
      rebuild_service "$2"
    else
      error "用法: $0 rebuild <service> (可选: $VALID_SERVICES)"
      exit 1
    fi
    ;;
  status)
    show_status
    ;;
  logs)
    show_logs "${2:-}"
    ;;
  build)
    check_dep docker
    compose build "${2:-}"
    ;;
  help|*)
    echo "用法: $0 {start|stop|restart|rebuild|status|logs|build|help}"
    echo ""
    echo "命令:"
    echo "  start              启动所有服务 (默认)"
    echo "  stop               停止所有服务"
    echo "  restart            重建并重启所有服务"
    echo "  restart <service>  重启指定组件 (postgres|redis|backend|frontend)"
    echo "  rebuild <service>  重建指定组件镜像 (不重启)"
    echo "  status             查看服务状态"
    echo "  logs [service]     查看日志 (可选指定服务名)"
    echo "  build [service]    重建镜像 (可选指定服务名)"
    echo "  help               显示帮助信息"
    ;;
esac
