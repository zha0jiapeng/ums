# Jenkins 快速开始指南

## 🚀 5分钟快速部署

### 1. 准备工作

确保您的Jenkins服务器已安装以下工具：
- ✅ JDK 8
- ✅ Maven 3.8.6
- ✅ Docker
- ✅ Git

### 2. 选择Jenkinsfile

根据您的需求选择：

| 文件 | 适用场景 | 特点 |
|------|----------|------|
| `Jenkinsfile-practical` | **推荐使用** | 实用版，包含完整流程 |
| `Jenkinsfile-simple` | 快速测试 | 基础构建和部署 |
| `Jenkinsfile-docker-compose` | 完整环境 | 包含MySQL+Redis |

### 3. 创建Jenkins任务

#### 步骤1：新建任务
```
1. 登录Jenkins
2. 点击"新建任务"
3. 输入任务名称：ums-server-pipeline
4. 选择"Pipeline"
5. 点击"确定"
```

#### 步骤2：配置流水线
```
Pipeline → Pipeline script from SCM
SCM → Git
Repository URL → 您的Git仓库地址
Branch Specifier → */main
Script Path → Jenkinsfile-practical
```

### 4. 修改配置

在Jenkinsfile中修改以下配置：

```groovy
environment {
    // 修改为您的服务器IP
    DEPLOY_HOST = '192.168.1.100'
    
    // 修改为您的部署用户
    DEPLOY_USER = 'deploy'
    
    // 修改为您的部署路径
    DEPLOY_PATH = '/opt/ums-server'
}
```

### 5. 运行流水线

#### 手动触发
```
1. 在Jenkins任务页面
2. 点击"立即构建"
3. 查看构建进度
```

#### 自动触发（推荐）
```
1. 推送代码到Git仓库
2. Jenkins自动检测并开始构建
3. 根据分支自动部署到对应环境
```

## 📋 分支策略

| 分支 | 部署环境 | 操作 |
|------|----------|------|
| `main/master` | 生产环境 | 构建 → Docker → 部署 → 确认 → 生产 |
| `develop` | 测试环境 | 构建 → Docker → 部署 → 测试 |
| 其他分支 | 仅构建 | 构建 → 测试 |

## 🔧 常见问题解决

### 问题1：Maven构建失败
```bash
# 检查Maven配置
mvn --version

# 清理并重新构建
mvn clean package -DskipTests
```

### 问题2：Docker构建失败
```bash
# 检查Docker服务
docker --version
docker ps

# 检查Dockerfile
docker build -t test-image .
```

### 问题3：部署失败
```bash
# 检查docker-compose
docker-compose --version

# 手动测试部署
docker-compose down
docker-compose up -d
```

## 📊 监控和日志

### 查看构建状态
- Jenkins任务页面 → 构建历史
- 点击具体构建 → 控制台输出

### 查看应用状态
```bash
# 查看容器状态
docker ps

# 查看应用日志
docker logs ums-server

# 查看所有服务日志
docker-compose logs
```

### 访问应用
- 测试环境：http://localhost:38080
- 生产环境：http://your-server-ip:38080

## 🎯 最佳实践

### 1. 环境分离
- 测试环境：develop分支
- 生产环境：main/master分支
- 使用不同配置文件

### 2. 安全配置
- 使用SSH密钥认证
- 定期更新密码
- 限制访问权限

### 3. 监控告警
- 设置构建失败通知
- 监控应用健康状态
- 配置日志收集

### 4. 备份策略
- 定期备份Jenkins配置
- 备份构建产物
- 备份数据库

## 📞 技术支持

如果遇到问题：
1. 查看Jenkins构建日志
2. 检查应用容器日志
3. 验证配置文件
4. 确认网络连接

## 🎉 成功标志

当您看到以下信息时，说明部署成功：
```
🎉 流水线执行成功！
📱 应用访问地址: http://localhost:38080
✅ 健康检查完成
``` 