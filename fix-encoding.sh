#!/bin/bash

# Jenkins文件编码修复脚本
# 用于解决Jenkinsfile的字符编码问题

echo "🔧 Jenkins文件编码修复工具"
echo "================================"

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

# 检查文件编码
check_file_encoding() {
    local file=$1
    echo "检查文件编码: $file"
    
    if [ -f "$file" ]; then
        # 检查文件编码
        file_encoding=$(file -bi "$file" | awk -F "=" '{print $2}')
        print_status "info" "文件编码: $file_encoding"
        
        # 检查是否包含非ASCII字符
        if grep -q -P '[^\x00-\x7F]' "$file"; then
            print_status "warning" "文件包含非ASCII字符，可能导致编码问题"
            return 1
        else
            print_status "success" "文件编码正常"
            return 0
        fi
    else
        print_status "error" "文件不存在: $file"
        return 1
    fi
}

# 修复文件编码
fix_file_encoding() {
    local file=$1
    local backup_file="${file}.backup"
    
    echo "修复文件编码: $file"
    
    # 创建备份
    cp "$file" "$backup_file"
    print_status "info" "已创建备份: $backup_file"
    
    # 转换为UTF-8编码
    if command -v iconv >/dev/null 2>&1; then
        iconv -f GBK -t UTF-8 "$backup_file" > "$file" 2>/dev/null || \
        iconv -f GB2312 -t UTF-8 "$backup_file" > "$file" 2>/dev/null || \
        iconv -f ISO-8859-1 -t UTF-8 "$backup_file" > "$file" 2>/dev/null
    fi
    
    # 检查修复结果
    if check_file_encoding "$file"; then
        print_status "success" "文件编码修复成功"
        return 0
    else
        print_status "error" "文件编码修复失败"
        # 恢复备份
        cp "$backup_file" "$file"
        return 1
    fi
}

# 创建UTF-8版本的Jenkinsfile
create_utf8_jenkinsfile() {
    local original_file="Jenkinsfile-docker-compose"
    local utf8_file="Jenkinsfile-docker-compose-utf8"
    
    echo "创建UTF-8版本的Jenkinsfile..."
    
    # 创建新的UTF-8文件
    cat > "$utf8_file" << 'EOF'
pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'ums-server'
        VERSION = '2.0'
        DOCKER_IMAGE = 'ums-server:2.0'
        DEPLOY_PATH = '/opt/ums-server'
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
                echo "Code checkout completed"
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
                    // Rename JAR file to match Dockerfile
                    sh 'cp target/ums-0.0.1-SNAPSHOT.jar ums-0.0.1-SNAPSHOT.jar'
                    
                    // Build Docker image
                    docker.build(DOCKER_IMAGE)
                }
            }
        }
        
        stage('Docker Compose Deploy') {
            steps {
                script {
                    // Deploy using docker-compose
                    sh """
                        # Stop existing services
                        docker-compose down || true
                        
                        # Start all services (MySQL, Redis, and application)
                        docker-compose up -d
                        
                        # Wait for services to start
                        sleep 30
                        
                        # Check service status
                        docker-compose ps
                    """
                }
            }
        }
        
        stage('Health Check') {
            steps {
                script {
                    // Wait for application to start
                    sh 'sleep 60'
                    
                    // Check application health
                    sh """
                        # Check container status
                        docker ps | grep ums-server
                        
                        # Check application port
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
                // Show container logs for debugging
                sh 'docker-compose logs ums-app || true'
            }
        }
    }
}
EOF

    print_status "success" "已创建UTF-8版本的Jenkinsfile: $utf8_file"
}

# 主函数
main() {
    echo "开始编码检查和修复..."
    echo ""
    
    # 检查当前目录下的Jenkinsfile
    for file in Jenkinsfile*; do
        if [ -f "$file" ]; then
            check_file_encoding "$file"
            echo ""
        fi
    done
    
    echo "🔧 修复选项："
    echo "1. 修复现有文件编码"
    echo "2. 创建UTF-8版本的Jenkinsfile"
    echo "3. 退出"
    
    read -p "请选择操作 (1-3): " choice
    
    case $choice in
        1)
            for file in Jenkinsfile*; do
                if [ -f "$file" ]; then
                    fix_file_encoding "$file"
                fi
            done
            ;;
        2)
            create_utf8_jenkinsfile
            ;;
        3)
            print_status "info" "退出修复工具"
            exit 0
            ;;
        *)
            print_status "error" "无效选择"
            exit 1
            ;;
    esac
    
    echo ""
    print_status "info" "修复完成！"
    print_status "info" "建议在Jenkins中使用UTF-8编码的文件"
}

# 运行主函数
main "$@" 