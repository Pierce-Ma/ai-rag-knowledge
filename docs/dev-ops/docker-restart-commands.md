# Docker 容器重启命令

## 方法 1：使用 docker-compose 重启（推荐）

### 重启所有服务
```bash
cd /dev-ops
docker-compose -f docker-compose-environment-aliyun.yml restart
```

### 重启特定服务（如只重启 PostgreSQL）
```bash
cd /dev-ops
docker-compose -f docker-compose-environment-aliyun.yml restart vector_db
```

### 停止并重新启动（会重新加载配置）
```bash
cd /dev-ops
docker-compose -f docker-compose-environment-aliyun.yml down
docker-compose -f docker-compose-environment-aliyun.yml up -d
```

## 方法 2：直接使用 docker 命令重启

### 重启单个容器
```bash
docker restart vector_db
```

### 停止并启动容器
```bash
docker stop vector_db
docker start vector_db
```

### 查看容器状态
```bash
docker ps -a | grep vector_db
```

## 方法 3：使用 docker compose（新版本，注意是空格不是横线）

```bash
cd /dev-ops
docker compose -f docker-compose-environment-aliyun.yml restart
```

## 常用命令

### 查看所有运行中的容器
```bash
docker ps
```

### 查看所有容器（包括停止的）
```bash
docker ps -a
```

### 查看容器日志
```bash
docker logs vector_db
docker logs -f vector_db  # 实时查看日志
```

### 进入容器
```bash
docker exec -it vector_db bash
```

