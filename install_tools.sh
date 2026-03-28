#!/bin/bash
# ========================================
# 一键安装开发环境工具
# 包含: Clash代理、Java JDK、Android SDK
# ========================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================"
echo "一键安装开发环境工具"
echo "========================================${NC}"

# 对象存储基础URL
TOS_BASE="https://coze-coding-project.tos.coze.site/coze_storage_7621867712305922067"

# ========================================
# 1. 安装 Clash 代理
# ========================================
install_clash() {
    echo -e "\n${YELLOW}[1/3] 安装 Clash 代理...${NC}"
    
    mkdir -p ~/clash && cd ~/clash
    
    # 下载 Clash 二进制
    if [ ! -f ~/clash/clash ]; then
        echo "下载 Clash 二进制..."
        wget -q "${TOS_BASE}/tools/clash/clash-linux-amd64_ef65dfcd?sign=2090073587-53d8e9fdbb-0-6e68d68600c0efafc3c854394b915b6b0f697eabb25ded199e0cb27efde47a17" -O clash
        chmod +x clash
    fi
    
    # 下载配置文件
    if [ ! -f ~/clash/config.yaml ]; then
        echo "下载 Clash 配置..."
        wget -q "${TOS_BASE}/tools/clash/config_0420c809.yaml?sign=2090073588-882c8e86d0-0-8dd66a8e5c6d62c972bf13d5c1da7668615d680ad5a06e8b9d47d5e5b12269e5" -O config.yaml
    fi
    
    # 启动 Clash
    echo "启动 Clash..."
    cd ~/clash
    nohup ./clash -d . > clash.log 2>&1 &
    sleep 3
    
    # 设置代理环境变量
    export http_proxy="http://127.0.0.1:7890"
    export https_proxy="http://127.0.0.1:7890"
    export all_proxy="socks5://127.0.0.1:7891"
    
    # 验证代理
    if curl -s --max-time 5 https://www.google.com > /dev/null; then
        echo -e "${GREEN}✓ Clash 安装成功，代理工作正常${NC}"
    else
        echo -e "${RED}✗ 代理可能未正常工作，请检查 clash.log${NC}"
    fi
}

# ========================================
# 2. 安装 Java JDK
# ========================================
install_java() {
    echo -e "\n${YELLOW}[2/3] 安装 Java JDK 17...${NC}"
    
    if command -v java &> /dev/null; then
        echo -e "${GREEN}✓ Java 已安装: $(java -version 2>&1 | head -1)${NC}"
        return
    fi
    
    apt-get update -qq
    apt-get install -y -qq openjdk-17-jdk-headless
    
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    export PATH=$JAVA_HOME/bin:$PATH
    
    echo -e "${GREEN}✓ Java 安装成功: $(java -version 2>&1 | head -1)${NC}"
}

# ========================================
# 3. 安装 Android SDK
# ========================================
install_android_sdk() {
    echo -e "\n${YELLOW}[3/3] 安装 Android SDK...${NC}"
    
    export ANDROID_HOME=/root/android-sdk
    
    if [ -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
        echo -e "${GREEN}✓ Android SDK 已安装${NC}"
        return
    fi
    
    mkdir -p $ANDROID_HOME/cmdline-tools
    cd /tmp
    
    # 下载 command-line tools
    echo "下载 Android SDK Command-line Tools..."
    wget -q "${TOS_BASE}/tools/android/commandlinetools-linux-11076708_8606417d.zip?sign=2090073598-ba373b415a-0-93ad166ef5a023b80450c1ab67d1e1eb4d90e67c380f57ab4f7c78a44e485e47" -O cmdline-tools.zip
    
    # 解压
    apt-get install -y -qq unzip > /dev/null 2>&1
    unzip -q -o cmdline-tools.zip
    mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest
    
    # 设置环境变量
    export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
    
    # 接受许可证
    yes | sdkmanager --licenses > /dev/null 2>&1
    
    # 安装必要组件
    echo "安装 SDK 组件..."
    sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2" > /dev/null 2>&1
    
    echo -e "${GREEN}✓ Android SDK 安装成功${NC}"
}

# ========================================
# 主流程
# ========================================
main() {
    # 检查是否为 root
    if [ "$EUID" -ne 0 ]; then
        echo -e "${RED}请使用 root 权限运行此脚本${NC}"
        exit 1
    fi
    
    install_clash
    install_java
    install_android_sdk
    
    echo -e "\n${GREEN}========================================"
    echo "安装完成！"
    echo "========================================${NC}"
    echo ""
    echo "环境变量已设置:"
    echo "  http_proxy=http://127.0.0.1:7890"
    echo "  https_proxy=http://127.0.0.1:7890"
    echo "  JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
    echo "  ANDROID_HOME=/root/android-sdk"
    echo ""
    echo "如需在后续命令中使用，请执行:"
    echo "  source ~/.bashrc  # 或重新登录"
}

main "$@"
