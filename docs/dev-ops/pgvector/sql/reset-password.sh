#!/bin/bash
# PostgreSQL 启动后自动重置密码脚本
# 此脚本会在容器启动后自动执行

set -e

echo "正在重置 PostgreSQL 密码..."

# 等待 PostgreSQL 完全启动
until pg_isready -U postgres -d postgres; do
  echo "等待 PostgreSQL 启动..."
  sleep 1
done

# 重置密码
psql -U postgres -d postgres <<EOF
ALTER USER postgres WITH PASSWORD 'postgres';
\q
EOF

echo "✅ 密码重置成功！"

