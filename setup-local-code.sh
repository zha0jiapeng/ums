#!/bin/bash

# 本地代码设置脚本
# 用于解决Jenkins网络连接问题

echo "🔧 本地代码设置工具"
echo "========================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 函数：打印带颜色的消息
print_status() {
    local status=$1
    local message=$2
    case $status in
        "success")
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        "error")
            echo -e "${RED}❌ $message${NC}"
            ;;
        "warning")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ;;
        "info")
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
    esac
}

# 创建本地代码目录
create_local_code_directory() {
    local local_code_path="/opt/ums-server-code"
    
    echo "创建本地代码目录..."
    
    # 创建目录
    if [ ! -d "$local_code_path" ]; then
        sudo mkdir -p "$local_code_path"
        print_status "success" "创建目录: $local_code_path"
    else
        print_status "info" "目录已存在: $local_code_path"
    fi
    
    # 设置权限
    sudo chown jenkins:jenkins "$local_code_path" 2>/dev/null || \
    sudo chown $USER:$USER "$local_code_path" 2>/dev/null || \
    sudo chmod 755 "$local_code_path"
    
    print_status "success" "目录权限设置完成"
    
    return 0
}

# 复制当前项目到本地目录
copy_project_to_local() {
    local local_code_path="/opt/ums-server-code"
    local current_dir=$(pwd)
    
    echo "复制项目到本地目录..."
    
    if [ ! -d "$local_code_path" ]; then
        create_local_code_directory
    fi
    
    # 清理本地目录
    sudo rm -rf "$local_code_path"/*
    print_status "info" "清理本地目录"
    
    # 复制项目文件
    sudo cp -r . "$local_code_path/"
    print_status "success" "项目文件复制完成"
    
    # 设置权限
    sudo chown -R jenkins:jenkins "$local_code_path" 2>/dev/null || \
    sudo chown -R $USER:$USER "$local_code_path" 2>/dev/null || \
    sudo chmod -R 755 "$local_code_path"
    
    print_status "success" "文件权限设置完成"
    
    # 显示复制结果
    echo ""
    print_status "info" "本地代码目录内容："
    ls -la "$local_code_path"
}

# 创建Jenkins本地任务配置
create_jenkins_local_config() {
    echo "创建Jenkins本地任务配置..."
    
    cat > "jenkins-local-config.txt" << 'EOF'
# Jenkins本地任务配置指南

## 任务配置步骤：

1. 创建新的Pipeline任务
2. 配置任务参数：
   - 任务名称: ums-server-local
   - 任务类型: Pipeline

3. Pipeline配置：
   - Definition: Pipeline script from SCM
   - SCM: None (留空)
   - Script Path: Jenkinsfile-local

4. 高级配置：
   - 在"Pipeline"部分选择"Pipeline script"
   - 直接粘贴以下脚本内容：

pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'ums-server'
        VERSION = '2.0'
        DOCKER_IMAGE = 'ums-server:2.0'
        DEPLOY_PATH = '/opt/ums-server'
        LOCAL_CODE_PATH = '/opt/ums-server-code'
    }
    
    stages {
        stage('Prepare Local Code') {
            steps {
                script {
                    if (!fileExists(LOCAL_CODE_PATH)) {
                        error "Local code directory does not exist: ${LOCAL_CODE_PATH}"
                    }
                    
                    sh """
                        echo "Copying local code to workspace..."
                        cp -r ${LOCAL_CODE_PATH}/* .
                        ls -la
                    """
                    
                    echo "Local code preparation completed"
                }
            }
        }
        
        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Docker Build') {
            steps {
                script {
                    sh 'cp target/ums-0.0.1-SNAPSHOT.jar ums-0.0.1-SNAPSHOT.jar'
                    docker.build(DOCKER_IMAGE)
                }
            }
        }
        
        stage('Docker Compose Deploy') {
            steps {
                script {
                    sh """
                        docker-compose down || true
                        docker-compose up -d
                        sleep 30
                        docker-compose ps
                    """
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    sh 'sleep 60'
                    sh """
                        docker ps | grep ums-server
                        curl -f http://localhost:38080/actuator/health || echo "Health check failed"
                    """
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
        success {
            echo "Deployment successful! Application running at http://localhost:38080"
        }
        failure {
            echo "Deployment failed!"
            script {
                sh 'docker-compose logs ums-app || true'
            }
        }
    }
}

## 注意事项：
1. 确保本地代码目录 /opt/ums-server-code 存在
2. 确保目录中包含完整的项目文件
3. 确保Jenkins用户有权限访问该目录
4. 如果使用Docker运行Jenkins，需要挂载本地目录

## 验证步骤：
1. 运行此脚本创建本地代码目录
2. 在Jenkins中创建新任务
3. 使用上述Pipeline脚本
4. 运行任务验证部署
EOF

    print_status "success" "Jenkins配置指南已创建: jenkins-local-config.txt"
}

# 验证本地代码
verify_local_code() {
    local local_code_path="/opt/ums-server-code"
    
    echo "验证本地代码..."
    
    if [ ! -d "$local_code_path" ]; then
        print_status "error" "本地代码目录不存在"
        return 1
    fi
    
    # 检查关键文件
    local required_files=("pom.xml" "Dockerfile" "docker-compose.yml" "Jenkinsfile-local")
    
    for file in "${required_files[@]}"; do
        if [ -f "$local_code_path/$file" ]; then
            print_status "success" "文件存在: $file"
        else
            print_status "warning" "文件缺失: $file"
        fi
    done
    
    # 显示目录结构
    echo ""
    print_status "info" "本地代码目录结构："
    find "$local_code_path" -type f -name "*.xml" -o -name "*.yml" -o -name "*.yaml" -o -name "Dockerfile" -o -name "Jenkinsfile*" | head -10
}

# 主函数
main() {
    echo "开始设置本地代码环境..."
    echo ""
    
    # 检查当前目录
    if [ ! -f "pom.xml" ]; then
        print_status "error" "当前目录不是Maven项目根目录"
        print_status "info" "请切换到项目根目录后重新运行此脚本"
        exit 1
    fi
    
    print_status "success" "检测到Maven项目"
    
    # 执行设置步骤
    create_local_code_directory
    copy_project_to_local
    create_jenkins_local_config
    verify_local_code
    
    echo ""
    print_status "success" "本地代码设置完成！"
    print_status "info" "请按照 jenkins-local-config.txt 中的指南配置Jenkins任务"
    print_status "info" "本地代码路径: /opt/ums-server-code"
}

# 检查权限
if [ "$EUID" -ne 0 ]; then
    print_status "warning" "建议使用sudo运行此脚本以确保正确的权限设置"
fi

# 运行主函数
main "$@" 