#!/bin/bash

# 加载项目环境变量脚本
# 使用方式: source ./load_env.sh 或 . ./load_env.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

eval $(python3 "$SCRIPT_DIR/load_env.py")
