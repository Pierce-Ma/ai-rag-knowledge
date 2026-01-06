#!/bin/bash

# 最终修复 PostgreSQL 密码的脚本
# 在服务器上执行此脚本

echo "=========================================="
echo "正在重置 PostgreSQL 密码..."
echo "=========================================="

# 方法1: 使用 docker exec 重置密码
echo "步骤 1: 重置 postgres 用户密码..."
docker exec -i vector_db psql -U postgres -d postgres <<EOF
ALTER USER postgres WITH PASSWORD 'postgres';
\q
EOF

if [ $? -eq 0 ]; then
    echo "✅ 密码重置命令执行成功"
else
    echo "❌ 密码重置失败，尝试方法2..."
    
    # 方法2: 如果方法1失败，尝试使用环境变量
    echo "步骤 2: 尝试使用 PGPASSWORD 环境变量..."
    docker exec -e PGPASSWORD=postgres -i vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"
fi

echo ""
echo "步骤 3: 验证密码是否重置成功..."
docker exec -i vector_db psql -U postgres -d ai-rag-knowledge -c "SELECT 'Connection successful!' as status, version() as pg_version;" 2>&1

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
else
    echo ""
    echo "=========================================="
    echo "❌ 验证失败，请检查："
    echo "1. 容器是否正在运行: docker ps | grep vector_db"
    echo "2. 容器日志: docker logs vector_db --tail 20"
    echo "=========================================="
fi

