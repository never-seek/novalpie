#!/usr/bin/env python3
"""
加载项目环境变量脚本
通过 coze_workload_identity.Client 获取项目环境变量并输出 export 语句
使用方式: eval $(python load_env.py)
"""

import os
import sys

# 添加 app 目录到 Python 路径
workspace_path = os.getenv("COZE_WORKSPACE_PATH", "/workspace/projects")
app_dir = os.path.join(workspace_path, 'src')
if app_dir not in sys.path:
    sys.path.insert(0, app_dir)

try:
    from coze_workload_identity import Client

    client = Client()
    env_vars = client.get_project_env_vars()
    client.close()

    # 输出 export 语句格式的环境变量
    for env_var in env_vars:
        # 转义特殊字符
        value = env_var.value.replace("'", "'\\''")
        print(f"export {env_var.key}='{value}'")

    # 输出成功消息到 stderr，不影响 eval
    print(f"# Successfully loaded {len(env_vars)} environment variables", file=sys.stderr)

except Exception as e:
    print(f"# Error loading environment variables: {e}", file=sys.stderr)
    sys.exit(1)
