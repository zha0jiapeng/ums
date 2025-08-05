#!/bin/bash

# Jenkins网络连接诊断和修复脚本
# 用于解决Jenkins连接Gitee等Git仓库的网络问题

echo "🔍 Jenkins网络连接诊断工具"
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

# 检查网络连通性
check_network_connectivity() {
    echo "1. 检查网络连通性..."
    
    # 测试基本网络连接
    if ping -c 3 gitee.com > /dev/null 2>&1; then
        print_status "success" "网络连通性正常"
    else
        print_status "error" "无法连接到gitee.com"
        return 1
    fi
    
    # 测试HTTPS端口
    if timeout 10 bash -c "</dev/tcp/gitee.com/443" > /dev/null 2>&1; then
        print_status "success" "HTTPS端口(443)连接正常"
    else
        print_status "error" "HTTPS端口(443)连接失败"
        return 1
    fi
    
    # 测试SSH端口
    if timeout 10 bash -c "</dev/tcp/gitee.com/22" > /dev/null 2>&1; then
        print_status "success" "SSH端口(22)连接正常"
    else
        print_status "error" "SSH端口(22)连接失败"
        return 1
    fi
}

# 检查DNS解析
check_dns_resolution() {
    echo "2. 检查DNS解析..."
    
    if nslookup gitee.com > /dev/null 2>&1; then
        print_status "success" "DNS解析正常"
        nslookup gitee.com | grep "Address:"
    else
        print_status "error" "DNS解析失败"
        return 1
    fi
}

# 检查Git配置
check_git_config() {
    echo "3. 检查Git配置..."
    
    # 检查Git版本
    git_version=$(git --version 2>/dev/null)
    if [ $? -eq 0 ]; then
        print_status "success" "Git已安装: $git_version"
    else
        print_status "error" "Git未安装"
        return 1
    fi
    
    # 检查Git配置
    git_config_http_proxy=$(git config --global --get http.proxy 2>/dev/null)
    git_config_https_proxy=$(git config --global --get https.proxy 2>/dev/null)
    
    if [ -n "$git_config_http_proxy" ]; then
        print_status "info" "HTTP代理配置: $git_config_http_proxy"
    fi
    
    if [ -n "$git_config_https_proxy" ]; then
        print_status "info" "HTTPS代理配置: $git_config_https_proxy"
    fi
}

# 测试Git连接
test_git_connection() {
    echo "4. 测试Git连接..."
    
    # 测试HTTPS连接
    print_status "info" "测试HTTPS连接..."
    if git ls-remote https://gitee.com/zhao_jiapeng/ums-server-new.git HEAD > /dev/null 2>&1; then
        print_status "success" "HTTPS连接成功"
    else
        print_status "error" "HTTPS连接失败"
    fi
    
    # 测试SSH连接
    print_status "info" "测试SSH连接..."
    if ssh -o ConnectTimeout=10 -o BatchMode=yes git@gitee.com exit 2>/dev/null; then
        print_status "success" "SSH连接成功"
    else
        print_status "warning" "SSH连接失败（可能需要配置SSH密钥）"
    fi
}

# 检查SSH配置
check_ssh_config() {
    echo "5. 检查SSH配置..."
    
    # 检查SSH密钥是否存在
    if [ -f ~/.ssh/id_rsa ]; then
        print_status "success" "SSH私钥存在"
        ls -la ~/.ssh/id_rsa
    else
        print_status "warning" "SSH私钥不存在"
    fi
    
    if [ -f ~/.ssh/id_rsa.pub ]; then
        print_status "success" "SSH公钥存在"
    else
        print_status "warning" "SSH公钥不存在"
    fi
    
    # 检查SSH代理
    if [ -n "$SSH_AUTH_SOCK" ]; then
        print_status "success" "SSH代理正在运行"
    else
        print_status "info" "SSH代理未运行"
    fi
}

# 生成SSH密钥
generate_ssh_key() {
    echo "6. 生成SSH密钥..."
    
    if [ -f ~/.ssh/id_rsa ]; then
        print_status "info" "SSH密钥已存在，跳过生成"
        return 0
    fi
    
    print_status "info" "生成新的SSH密钥..."
    ssh-keygen -t rsa -b 4096 -C "jenkins@gitee.com" -f ~/.ssh/id_rsa -N ""
    
    if [ $? -eq 0 ]; then
        print_status "success" "SSH密钥生成成功"
        
        # 启动SSH代理并添加密钥
        eval "$(ssh-agent -s)" > /dev/null 2>&1
        ssh-add ~/.ssh/id_rsa > /dev/null 2>&1
        
        print_status "info" "SSH公钥内容："
        cat ~/.ssh/id_rsa.pub
        echo ""
        print_status "warning" "请将此公钥添加到Gitee账户的SSH公钥设置中"
    else
        print_status "error" "SSH密钥生成失败"
        return 1
    fi
}

# 配置Git代理（如果需要）
configure_git_proxy() {
    echo "7. 配置Git代理..."
    
    read -p "是否需要配置代理？(y/n): " configure_proxy
    
    if [ "$configure_proxy" = "y" ] || [ "$configure_proxy" = "Y" ]; then
        read -p "请输入代理服务器地址 (例如: proxy.company.com:8080): " proxy_server
        
        if [ -n "$proxy_server" ]; then
            git config --global http.proxy "http://$proxy_server"
            git config --global https.proxy "https://$proxy_server"
            print_status "success" "Git代理配置完成: $proxy_server"
        fi
    else
        print_status "info" "跳过代理配置"
    fi
}

# 主函数
main() {
    echo "开始网络诊断..."
    echo ""
    
    # 执行各项检查
    check_network_connectivity
    check_dns_resolution
    check_git_config
    test_git_connection
    check_ssh_config
    
    echo ""
    echo "🔧 修复选项："
    echo "1. 生成SSH密钥"
    echo "2. 配置Git代理"
    echo "3. 退出"
    
    read -p "请选择操作 (1-3): " choice
    
    case $choice in
        1)
            generate_ssh_key
            ;;
        2)
            configure_git_proxy
            ;;
        3)
            print_status "info" "退出诊断工具"
            exit 0
            ;;
        *)
            print_status "error" "无效选择"
            exit 1
            ;;
    esac
    
    echo ""
    print_status "info" "诊断完成！"
    print_status "info" "如果问题仍然存在，请检查："
    print_status "info" "1. 防火墙设置"
    print_status "info" "2. 网络代理配置"
    print_status "info" "3. Jenkins系统配置"
}

# 检查是否以root权限运行
if [ "$EUID" -eq 0 ]; then
    print_status "warning" "检测到root权限，建议使用普通用户运行此脚本"
fi

# 运行主函数
main "$@" 