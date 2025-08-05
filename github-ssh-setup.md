# GitHub SSH连接配置指南

## 🚀 快速解决方案

### 步骤1：生成SSH密钥
在Jenkins服务器上执行：
```bash
# 生成SSH密钥对
ssh-keygen -t rsa -b 4096 -C "jenkins@github.com"

# 查看公钥内容
cat ~/.ssh/id_rsa.pub
```

### 步骤2：添加SSH公钥到GitHub
1. 登录GitHub账户
2. 点击右上角头像 → Settings
3. 左侧菜单选择"SSH and GPG keys"
4. 点击"New SSH key"
5. 标题：Jenkins Server
6. Key：粘贴刚才生成的公钥内容
7. 点击"Add SSH key"

### 步骤3：测试SSH连接
```bash
# 测试SSH连接
ssh -T git@github.com
```

### 步骤4：在Jenkins中配置SSH凭据
1. 进入 Jenkins 管理 → 凭据 → 系统 → 全局凭据
2. 点击"添加凭据"
3. 选择类型：SSH Username with private key
4. 配置：
   - ID: github-ssh-key
   - 描述: GitHub SSH Key
   - 用户名: git
   - 私钥: 选择"From a file on jenkins master"
   - 文件路径: ~/.ssh/id_rsa

### 步骤5：修改Jenkins任务配置
1. 编辑您的Jenkins任务
2. 在Pipeline配置中：
   - Repository URL: `git@github.com:zha0jiapeng/ums.git`
   - Credentials: 选择刚才创建的SSH凭据

## 🔧 详细配置步骤

### 1. 检查SSH密钥是否存在
```bash
ls -la ~/.ssh/
```

### 2. 如果密钥不存在，生成新密钥
```bash
ssh-keygen -t rsa -b 4096 -C "jenkins@github.com"
# 按回车接受默认路径
# 可以设置密码，也可以直接回车不设置密码
```

### 3. 启动SSH代理并添加密钥
```bash
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_rsa
```

### 4. 复制公钥内容
```bash
cat ~/.ssh/id_rsa.pub
```

### 5. 在GitHub中添加SSH公钥
- 登录GitHub
- 点击右上角头像 → Settings
- 左侧菜单选择"SSH and GPG keys"
- 点击"New SSH key"
- Title：Jenkins Server
- Key：粘贴刚才复制的公钥内容
- 点击"Add SSH key"

### 6. 测试连接
```bash
ssh -T git@github.com
# 如果成功，会显示：Hi username! You've successfully authenticated...
```

## 📋 验证清单

- [ ] SSH密钥已生成
- [ ] 公钥已添加到GitHub
- [ ] SSH连接测试成功
- [ ] Jenkins中已配置SSH凭据
- [ ] Jenkins任务使用SSH URL

## 🆘 常见问题

### 问题1：SSH连接被拒绝
```bash
# 检查SSH配置
ssh -vT git@github.com
```

### 问题2：权限被拒绝
```bash
# 检查密钥权限
chmod 600 ~/.ssh/id_rsa
chmod 644 ~/.ssh/id_rsa.pub
```

### 问题3：Jenkins无法找到SSH密钥
确保Jenkins用户有权限访问SSH密钥文件：
```bash
# 如果Jenkins运行在Docker中，需要挂载SSH目录
# 或者将密钥复制到Jenkins工作目录
```

## 🔄 使用本地代码方案

如果SSH连接仍然有问题，可以使用本地代码方案：

1. 将代码复制到Jenkins服务器本地目录
2. 使用 `Jenkinsfile-local` 文件
3. 修改 `LOCAL_CODE_PATH` 变量为实际路径

```bash
# 创建本地代码目录
mkdir -p /opt/ums-server-code
# 将项目代码复制到该目录
cp -r /path/to/your/project/* /opt/ums-server-code/
``` 