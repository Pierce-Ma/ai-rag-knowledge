#!/bin/bash

# PostgreSQL 密码重置脚本
# 使用方法: ./fix-postgres-password.sh

echo "正在重置 PostgreSQL 密码..."

# 进入容器并重置密码
docker exec -it vector_db psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"

if [ $? -eq 0 ]; then
    echo "✅ 密码重置成功！"
    echo "现在可以使用以下凭据连接："
    echo "  用户名: postgres"
    echo "  密码: postgres"
else
    echo "❌ 密码重置失败，请检查容器是否正在运行"
    echo "尝试使用以下命令检查容器状态："
    echo "  docker ps | grep vector_db"
fi

