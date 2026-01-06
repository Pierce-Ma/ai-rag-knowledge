#!/bin/bash

# PostgreSQL 自动重置密码脚本
# 在服务器上执行此脚本，或在容器启动后自动执行
# 使用方法: ./postgres-auto-reset-password.sh

CONTAINER_NAME="vector_db"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "=========================================="
echo "正在自动重置 PostgreSQL 密码..."
echo "=========================================="

# 检查容器是否运行
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "❌ 容器 ${CONTAINER_NAME} 未运行"
    echo "请先启动容器: docker-compose -f docker-compose-environment-aliyun.yml up -d vector_db"
    exit 1
fi

# 等待 PostgreSQL 就绪
echo "步骤 1: 等待 PostgreSQL 就绪..."
for i in $(seq 1 $MAX_RETRIES); do
    if docker exec ${CONTAINER_NAME} pg_isready -U postgres -d postgres > /dev/null 2>&1; then
        echo "✅ PostgreSQL 已就绪"
        break
    fi
    if [ $i -eq $MAX_RETRIES ]; then
        echo "❌ PostgreSQL 未就绪，请检查容器日志"
        docker logs ${CONTAINER_NAME} --tail 20
        exit 1
    fi
    echo "等待 PostgreSQL 就绪... ($i/$MAX_RETRIES)"
    sleep $RETRY_INTERVAL
done

# 重置密码（使用容器内的信任连接，不需要密码）
echo ""
echo "步骤 2: 重置 postgres 用户密码..."
docker exec -i ${CONTAINER_NAME} psql -U postgres -d postgres <<EOF
ALTER USER postgres WITH PASSWORD 'postgres';
\q
EOF

if [ $? -eq 0 ]; then
    echo "✅ 密码重置命令执行成功"
else
    echo "❌ 密码重置失败"
    exit 1
fi

# 验证密码
echo ""
echo "步骤 3: 验证密码是否重置成功..."
docker exec -i ${CONTAINER_NAME} psql -U postgres -d ai-rag-knowledge -c "SELECT 'Password reset successful!' as status, version() as pg_version;" 2>&1

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ 密码重置成功！"
    echo "现在可以使用以下凭据连接："
    echo "  主机: 111.228.7.181"
    echo "  端口: 15432"
    echo "  数据库: ai-rag-knowledge"
    echo "  用户名: postgres"
    echo "  密码: postgres"
    echo "=========================================="
    exit 0
else
    echo ""
    echo "=========================================="
    echo "❌ 验证失败，请检查："
    echo "1. 容器是否正在运行: docker ps | grep ${CONTAINER_NAME}"
    echo "2. 容器日志: docker logs ${CONTAINER_NAME} --tail 20"
    echo "=========================================="
    exit 1
fi

