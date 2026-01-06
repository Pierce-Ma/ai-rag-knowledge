# 修复远程 PostgreSQL 密码问题

## 问题分析

应用在**本地 macOS**运行，数据库在**远程服务器** `111.228.7.181:15432`。

PostgreSQL Docker 容器的密码重置需要注意：
- `POSTGRES_PASSWORD` 环境变量只在**首次初始化**时生效
- 如果数据卷已存在，重启容器**不会**重置密码
- 需要手动在容器内重置密码

## 解决方案

### 方案 1：在服务器上再次重置密码（推荐）

在服务器上执行：

```bash
# 进入容器并重置密码
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"

# 验证密码是否重置成功
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT version();"
```

### 方案 2：从本地测试连接远程数据库

在本地 macOS 上测试连接（需要安装 PostgreSQL 客户端）：

```bash
# 如果安装了 PostgreSQL 客户端
psql -h 111.228.7.181 -p 15432 -U postgres -d ai-rag-knowledge

# 或者使用 telnet 测试端口是否开放
telnet 111.228.7.181 15432
```

### 方案 3：检查容器状态和日志

在服务器上检查：

```bash
# 查看容器状态
docker ps | grep vector_db

# 查看容器日志
docker logs vector_db --tail 50

# 检查容器是否重启过
docker inspect vector_db | grep RestartCount
```

### 方案 4：重新创建容器（会丢失数据）

如果数据不重要，可以重新创建：

```bash
cd /dev-ops
docker-compose -f docker-compose-environment-aliyun.yml stop vector_db
docker-compose -f docker-compose-environment-aliyun.yml rm -f vector_db
# 删除数据卷（可选，会丢失所有数据）
docker volume ls | grep pgvector
# docker volume rm <volume_name>
docker-compose -f docker-compose-environment-aliyun.yml up -d vector_db
```

## 快速修复命令（在服务器上执行）

```bash
# 一键重置密码并验证
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';" && \
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT 'Password reset successful!' as status;"
```

## 常见问题

### Q: 为什么重置密码后还是连接失败？
A: 可能原因：
1. 容器重启后密码被重置（如果数据卷不存在）
2. 有多个数据库实例
3. 网络连接问题
4. 防火墙阻止了连接

### Q: 如何确保密码永久生效？
A: 在 docker-compose 文件中确保 `POSTGRES_PASSWORD` 环境变量存在，并且数据卷是持久化的。

