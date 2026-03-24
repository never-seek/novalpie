#!/bin/bash

set -e

mode=""
node=""
input=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="${COZE_WORKSPACE_PATH:-$(dirname "$SCRIPT_DIR")}"

usage() {
  echo "用法: $0 -m <模式> [-n <节点ID>] [-i <输入JSON>]"
  echo ""
  echo "参数说明:"
  echo "  -m <模式>        运行模式: http, flow, node, agent"
  echo "  -n <节点ID>      节点ID (仅在 node 模式下需要)"
  echo "  -i <输入JSON>    输入数据，支持 JSON 字符串或纯文本"
  echo "  -h              显示帮助信息"
  echo ""
  echo "示例:"
  echo "  $0 -m flow"
  echo "  $0 -m flow -i '{\"text\": \"你好\"}'"
  echo "  $0 -m flow -i '你好'"
  echo "  $0 -m node -n node_1 -i '{\"text\": \"测试\"}'"
}

while getopts "m:n:i:h" opt; do
  case "$opt" in
    m)
      mode="$OPTARG"
      ;;
    n)
      node="$OPTARG"
      ;;
    i)
      input="$OPTARG"
      ;;
    h)
      usage
      exit 0
      ;;
    \?)
      echo "无效选项: -$OPTARG"
      usage
      exit -1
      ;;
  esac
done

if [ -z "$mode" ]; then
  echo "错误: 必须指定 -m 参数"
  usage
  exit -1
fi


# Load environment variables
if [ -f "${SCRIPT_DIR}/load_env.sh" ]; then
  echo "Loading environment variables..."
  source "${SCRIPT_DIR}/load_env.sh"
fi

# Build python command
cmd="python ${WORK_DIR}/src/main.py -m \"$mode\""

if [ -n "$node" ]; then
  cmd="$cmd -n \"$node\""
fi

if [ -n "$input" ]; then
  cmd="$cmd -i '$input'"
fi

# Execute command
echo "Executing: $cmd"
eval $cmd
