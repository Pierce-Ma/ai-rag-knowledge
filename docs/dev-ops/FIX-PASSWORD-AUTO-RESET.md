# PostgreSQL 密码自动重置方案

## 问题分析

从 Portainer 可以看到，数据卷已经存在：
- `/var/lib/postgresql/data` 已挂载到 Docker volume（ID: `2b103989a4b7d17203e026051a27454cdaadd483efd66c36c160dbfd5c210215`）

**但是**，即使有数据卷，`POSTGRES_PASSWORD` 环境变量只在**首次初始化**时生效。如果数据卷已存在，这个变量会被忽略，密码不会自动重置。

## 解决方案

已创建自动重置密码脚本：`postgres-auto-reset-password.sh`

### 使用方法

#### 方法 1：手动执行（推荐）

在服务器上执行：

```bash
cd /dev-ops

# 1. 给脚本添加执行权限
chmod +x postgres-auto-reset-password.sh

# 2. 执行脚本
./postgres-auto-reset-password.sh
```

#### 方法 2：容器启动后自动执行

可以创建一个 systemd 服务或 cron 任务，在容器启动后自动执行：

```bash
# 创建 systemd 服务（可选）
sudo nano /etc/systemd/system/postgres-password-reset.service
```

服务内容：
```ini
[Unit]
Description=PostgreSQL Password Auto Reset
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
ExecStart=/dev-ops/postgres-auto-reset-password.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
```

### 验证修复

```bash
# 1. 测试数据库连接
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT version();"

# 2. 从本地测试连接（如果安装了 PostgreSQL 客户端）
# psql -h 111.228.7.181 -p 15432 -U postgres -d ai-rag-knowledge
```

## 工作原理

脚本使用 `docker exec` 在容器内执行 SQL 命令：
1. 检查容器是否运行
2. 等待 PostgreSQL 就绪
3. 使用容器内的信任连接（不需要密码）执行 `ALTER USER postgres WITH PASSWORD 'postgres';`
4. 验证密码是否重置成功

## 为什么使用容器内执行？

- **容器内的本地连接使用 `trust` 认证**，不需要密码
- **外部连接使用 `scram-sha-256` 认证**，需要密码
- 因此，即使密码不对，也可以在容器内重置密码

## 手动重置密码（如果脚本失败）

如果脚本失败，可以手动执行：

```bash
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"
```

