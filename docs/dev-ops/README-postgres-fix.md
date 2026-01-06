# PostgreSQL 密码认证失败问题修复指南

## 问题描述
应用启动时出现错误：`FATAL: password authentication failed for user "postgres"`

## 可能原因
1. 数据库容器已运行一段时间，密码可能被修改过
2. 数据库初始化时密码设置不正确
3. 配置文件中的密码与实际数据库密码不匹配

## 解决方案

### 方案 1：重置数据库密码（推荐，保留数据）

```bash
# 进入容器并重置密码
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"

# 或者使用脚本
chmod +x docs/dev-ops/fix-postgres-password.sh
./docs/dev-ops/fix-postgres-password.sh
```

### 方案 2：测试当前密码是否正确

```bash
# 测试连接
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge

# 如果提示输入密码，说明密码不是 "postgres"
# 如果直接进入，说明密码正确，问题可能在应用配置
```

### 方案 3：重新创建数据库容器（会丢失数据）

如果数据不重要，可以重新创建容器：

```bash
# 停止并删除容器
docker stop vector_db
docker rm vector_db

# 删除数据卷（可选，会丢失所有数据）
docker volume ls | grep pgvector
# docker volume rm <volume_name>

# 重新启动容器
cd docs/dev-ops
docker-compose -f docker-compose-environment-aliyun.yml up -d vector_db
```

### 方案 4：检查配置文件

确认 `application-dev.yml` 中的配置：

```yaml
spring:
  datasource:
    username: postgres
    password: postgres
    url: jdbc:postgresql://111.228.7.181:15432/ai-rag-knowledge
```

### 方案 5：使用环境变量覆盖密码

如果数据库密码确实不是 "postgres"，可以在启动应用时使用环境变量：

```bash
export SPRING_DATASOURCE_PASSWORD=你的实际密码
# 然后启动应用
```

或者在 IDE 的运行配置中添加环境变量：
- `SPRING_DATASOURCE_PASSWORD=你的实际密码`

## 验证修复

修复后，重新启动应用，应该能够正常连接数据库。

## 注意事项

1. 如果使用方案 1 重置密码，确保应用配置中的密码与数据库密码一致
2. 方案 3 会丢失所有数据，请谨慎使用
3. 生产环境建议使用更强的密码，不要使用默认的 "postgres"

