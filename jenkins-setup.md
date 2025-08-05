# Jenkins 流水线配置说明

## 1. Jenkins 环境要求

### 必需插件
- Pipeline
- Git
- Docker Pipeline
- SSH Agent
- SonarQube Scanner (可选)
- Workspace Cleanup

### 必需工具
- JDK 8
- Maven 3.8.6
- Docker
- Git

## 2. Jenkins 全局工具配置

### 配置 JDK
1. 进入 Jenkins 管理 → 全局工具配置
2. 添加 JDK 安装：
   - 名称：`JDK-8`
   - JAVA_HOME：`/usr/lib/jvm/java-8-openjdk-amd64` (Linux) 或 `C:\Program Files\Java\jdk1.8.0_xxx` (Windows)

### 配置 Maven
1. 添加 Maven 安装：
   - 名称：`Maven-3.8.6`
   - MAVEN_HOME：`/usr/share/maven` (Linux) 或 `C:\Program Files\Apache\maven` (Windows)

### 配置 Docker
1. 确保 Jenkins 用户有 Docker 权限
2. 将 Jenkins 用户添加到 docker 组：
   ```bash
   sudo usermod -aG docker jenkins
   sudo systemctl restart jenkins
   ```

## 3. 凭据配置

### SSH 密钥配置
1. 进入 Jenkins 管理 → 凭据 → 系统 → 全局凭据
2. 添加 SSH 私钥：
   - 类型：SSH Username with private key
   - ID：`test-server-ssh-key` (测试环境)
   - ID：`prod-server-ssh-key` (生产环境)
   - 用户名：`deploy`
   - 私钥：选择 From a file on jenkins master

### Docker Registry 凭据 (可选)
1. 添加 Docker Registry 凭据：
   - 类型：Username with password
   - ID：`docker-registry-credentials`
   - 用户名和密码：您的 Docker Registry 凭据

## 4. 流水线脚本选择

### 完整版 (Jenkinsfile)
适用于企业级环境，包含：
- 代码质量检查 (SonarQube)
- 单元测试
- 多环境部署
- 生产环境确认
- 完整的错误处理

### 简化版 (Jenkinsfile-simple)
适用于快速部署，包含：
- 基本的 Maven 构建
- Docker 镜像构建
- 简单的容器部署

### Docker Compose 版 (Jenkinsfile-docker-compose)
适用于完整环境部署，包含：
- Maven 构建
- Docker 镜像构建
- 使用 docker-compose 部署完整环境 (MySQL + Redis + 应用)
- 健康检查

## 5. 使用说明

### 创建流水线任务
1. 新建任务 → 选择 Pipeline
2. 配置 Git 仓库地址
3. 选择 Pipeline script from SCM
4. 选择 Git
5. 指定 Jenkinsfile 路径

### 环境变量配置
根据您的实际环境修改以下变量：
- `DEPLOY_HOST`：部署服务器地址
- `DEPLOY_USER`：部署用户
- `DEPLOY_PATH`：部署路径
- `DOCKER_IMAGE_NAME`：Docker 镜像名称
- `PROJECT_VERSION`：项目版本

### 分支策略
- `main/master`：生产环境部署
- `develop`：测试环境部署
- 其他分支：仅构建，不部署

## 6. 故障排除

### 常见问题
1. **Maven 构建失败**
   - 检查 Maven 配置
   - 确认网络连接
   - 检查 pom.xml 文件

2. **Docker 构建失败**
   - 确认 Docker 服务运行
   - 检查 Dockerfile 语法
   - 确认 JAR 文件存在

3. **部署失败**
   - 检查 SSH 连接
   - 确认目标服务器可访问
   - 检查端口是否被占用

### 日志查看
- 构建日志：Jenkins 构建页面
- 应用日志：`docker logs ums-server`
- 系统日志：`docker-compose logs`

## 7. 安全建议

1. 使用 HTTPS 访问 Jenkins
2. 定期更新 Jenkins 和插件
3. 限制 Jenkins 用户权限
4. 使用强密码和 SSH 密钥
5. 定期备份 Jenkins 配置 