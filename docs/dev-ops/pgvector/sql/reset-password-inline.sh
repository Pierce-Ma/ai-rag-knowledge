#!/bin/sh
# 在容器内执行的密码重置脚本
# 使用容器内的信任连接，不需要密码

set -e

echo "等待 PostgreSQL 启动..."
until pg_isready -U postgres -d postgres; do
  sleep 1
done

echo "正在重置 PostgreSQL 密码..."
psql -U postgres -d postgres <<EOF
ALTER USER postgres WITH PASSWORD 'postgres';
\q
EOF

echo "✅ 密码重置成功！"

