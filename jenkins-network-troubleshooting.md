# Jenkins 网络连接故障排除指南

## 🚨 问题描述
```
Command "git ls-remote -h -- https://gitee.com/zhao_jiapeng/ums-server-new.git HEAD" returned status code 128:
fatal: unable to access 'https://gitee.com/zhao_jiapeng/ums-server-new.git/': Failed to connect to gitee.com port 443
```

## 🔍 问题分析
这是典型的网络连接问题，可能的原因：
1. 网络防火墙阻止了443端口
2. DNS解析问题
3. 代理设置问题
4. Gitee服务器暂时不可用

## 🛠️ 解决步骤

### 步骤1：检查网络连通性

#### 在Jenkins服务器上执行：
```bash
# 测试网络连通性
ping gitee.com

# 测试端口连通性
telnet gitee.com 443

# 或者使用curl测试
curl -I https://gitee.com
```

#### 如果无法连接，尝试：
```bash
# 检查DNS解析
nslookup gitee.com

# 使用IP地址测试
ping 212.64.62.174  # Gitee的IP地址
```

### 步骤2：配置代理（如果需要）

#### 如果Jenkins在代理环境中：
```bash
# 设置Git代理
git config --global http.proxy http://proxy-server:port
git config --global https.proxy https://proxy-server:port

# 或者在Jenkins系统配置中设置
```

#### 在Jenkins中配置代理：
1. 进入 Jenkins 管理 → 系统配置
2. 找到"全局属性"部分
3. 添加环境变量：
   - `HTTP_PROXY=http://proxy-server:port`
   - `HTTPS_PROXY=https://proxy-server:port`

### 步骤3：配置Git凭据

#### 方法1：使用用户名密码
1. 进入 Jenkins 管理 → 凭据 → 系统 → 全局凭据
2. 添加凭据：
   - 类型：Username with password
   - 用户名：您的Gitee用户名
   - 密码：您的Gitee密码或访问令牌
   - ID：gitee-credentials

#### 方法2：使用SSH密钥（推荐）
1. 生成SSH密钥：
```bash
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"
```

2. 将公钥添加到Gitee：
   - 复制 `~/.ssh/id_rsa.pub` 内容
   - 在Gitee设置中添加SSH公钥

3. 在Jenkins中添加SSH凭据：
   - 类型：SSH Username with private key
   - 用户名：git
   - 私钥：选择From a file on jenkins master

### 步骤4：修改仓库URL

#### 使用SSH URL（推荐）：
```
git@gitee.com:zhao_jiapeng/ums-server-new.git
```

#### 使用HTTPS URL（需要凭据）：
```
https://gitee.com/zhao_jiapeng/ums-server-new.git
```

### 步骤5：临时解决方案

#### 如果网络问题无法立即解决：
1. 使用本地文件系统：
   - 将代码复制到Jenkins服务器本地目录
   - 使用本地路径作为仓库地址

2. 使用其他Git服务：
   - GitHub
   - GitLab
   - 自建Git服务器

## 🔧 具体操作步骤

### 1. 立即解决方案
```bash
# 在Jenkins服务器上测试连接
ssh -T git@gitee.com

# 如果成功，修改Jenkins配置使用SSH URL
```

### 2. 修改Jenkins任务配置
1. 编辑Jenkins任务
2. 在Pipeline配置中：
   - Repository URL: `git@gitee.com:zhao_jiapeng/ums-server-new.git`
   - Credentials: 选择SSH凭据

### 3. 验证配置
```bash
# 在Jenkins服务器上手动测试
git clone git@gitee.com:zhao_jiapeng/ums-server-new.git test-repo
```

## 📋 检查清单

- [ ] 网络连通性测试通过
- [ ] DNS解析正常
- [ ] 代理配置正确（如果需要）
- [ ] Git凭据配置正确
- [ ] SSH密钥已添加到Gitee
- [ ] Jenkins任务使用正确的仓库URL

## 🆘 如果问题仍然存在

1. **联系网络管理员**：检查防火墙设置
2. **使用VPN**：如果在内网环境
3. **更换Git服务**：临时使用其他Git服务
4. **本地部署**：将代码直接放在Jenkins服务器上

## 📞 技术支持

如果以上方法都无法解决问题，请提供：
1. Jenkins服务器操作系统信息
2. 网络环境描述（内网/外网/代理）
3. 完整的错误日志
4. 网络连通性测试结果 