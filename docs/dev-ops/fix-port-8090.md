# 解决端口 8090 被占用问题

## 问题
应用启动失败：`Port 8090 was already in use`

## 解决方案

### 方案 1：停止占用端口的进程（推荐）

```bash
# 查看占用端口的进程详情
lsof -i:8090

# 或者使用
netstat -anv | grep 8090

# 停止进程（PID 是 894）
kill 894

# 如果进程不停止，强制停止
kill -9 894
```

### 方案 2：修改应用端口

编辑 `xfg-dev-tech-app/src/main/resources/application-dev.yml`：

```yaml
server:
  port: 8091  # 改为其他端口，如 8091
```

### 方案 3：在 IDE 中停止之前的运行实例

如果是在 IntelliJ IDEA 中运行：
1. 查看运行窗口，找到之前运行的实例
2. 点击停止按钮停止之前的进程
3. 重新运行应用

## 快速修复命令

```bash
# 一键停止占用 8090 端口的进程
kill $(lsof -ti:8090)

# 或者强制停止
kill -9 $(lsof -ti:8090)
```

