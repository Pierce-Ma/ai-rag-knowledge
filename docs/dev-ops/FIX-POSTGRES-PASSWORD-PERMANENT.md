# PostgreSQL 密码问题永久修复方案

## 问题根源

**关键问题：** `vector_db` 服务没有定义数据卷来持久化 PostgreSQL 数据，导致：
1. 容器重启后，数据可能不一致
2. `POSTGRES_PASSWORD` 环境变量只在首次初始化时生效，如果数据已存在会被忽略
3. 密码可能被重置或丢失

## 解决方案

### 步骤 1：更新 docker-compose 文件（已完成）

已在 `docker-compose-environment-aliyun.yml` 中添加：
- 数据卷：`pgvector_data:/var/lib/postgresql/data` - 持久化 PostgreSQL 数据
- volumes 定义：确保数据卷被正确管理

### 步骤 2：在服务器上应用修复

**⚠️ 重要：** 如果容器已经在运行，需要先备份数据，然后重新创建容器。

#### 方案 A：保留现有数据（推荐）

```bash
# 1. 进入服务器 dev-ops 目录
cd /dev-ops

# 2. 备份当前数据（如果容器正在运行）
docker exec vector_db pg_dump -U postgres ai-rag-knowledge > backup_$(date +%Y%m%d_%H%M%S).sql

# 3. 停止容器
docker-compose -f docker-compose-environment-aliyun.yml stop vector_db

# 4. 创建数据卷（如果不存在）
docker volume create pgvector_data

# 5. 如果容器内已有数据，需要迁移到数据卷
# 先启动容器，然后执行：
docker-compose -f docker-compose-environment-aliyun.yml up -d vector_db

# 6. 等待容器启动后，重置密码
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"

# 7. 验证密码
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT version();"
```

#### 方案 B：重新创建容器（会丢失数据）

如果数据不重要，可以重新创建：

```bash
cd /dev-ops

# 1. 停止并删除容器
docker-compose -f docker-compose-environment-aliyun.yml stop vector_db
docker-compose -f docker-compose-environment-aliyun.yml rm -f vector_db

# 2. 删除旧的数据（可选，会丢失所有数据）
docker volume rm pgvector_data 2>/dev/null || true

# 3. 使用更新后的 docker-compose 启动
docker-compose -f docker-compose-environment-aliyun.yml up -d vector_db

# 4. 等待容器启动后，验证密码
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT version();"
```

### 步骤 3：验证修复

```bash
# 1. 检查数据卷是否创建
docker volume ls | grep pgvector_data

# 2. 检查容器状态
docker ps | grep vector_db

# 3. 测试数据库连接
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT 'Connection successful!' as status;"

# 4. 从本地测试连接（需要安装 PostgreSQL 客户端）
# psql -h 111.228.7.181 -p 15432 -U postgres -d ai-rag-knowledge
```

## 为什么会出现这个问题？

1. **PostgreSQL Docker 镜像的行为：**
   - `POSTGRES_PASSWORD` 环境变量只在**首次初始化数据库**时生效
   - 如果数据目录已存在（即使没有持久化），这个变量会被忽略
   - 容器重启时，如果数据目录状态不一致，可能导致密码问题

2. **没有数据卷的后果：**
   - 数据存储在容器内部，容器删除后数据丢失
   - 容器重启时，数据状态可能不一致
   - 密码可能被重置或丢失

## 修复后的优势

✅ **数据持久化：** 数据存储在 Docker 卷中，容器重启不会丢失数据  
✅ **密码稳定：** 密码存储在持久化的数据目录中，不会丢失  
✅ **易于备份：** 可以备份整个数据卷  
✅ **易于迁移：** 可以轻松迁移到其他服务器

## 后续维护

如果将来再次遇到密码问题，可以使用自动修复脚本：

```bash
# 在服务器上执行
cd /dev-ops
chmod +x auto-fix-postgres-password.sh
./auto-fix-postgres-password.sh
```

