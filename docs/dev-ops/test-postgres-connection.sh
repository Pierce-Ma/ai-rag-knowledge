#!/bin/bash

# PostgreSQL 连接测试脚本
# 使用方法: ./test-postgres-connection.sh

echo "正在测试 PostgreSQL 连接..."

# 测试连接
docker exec -it vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT version();"

if [ $? -eq 0 ]; then
    echo "✅ 连接成功！数据库密码正确。"
else
    echo "❌ 连接失败！可能需要重置密码。"
    echo "请运行: ./fix-postgres-password.sh"
fi

