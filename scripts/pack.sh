#!/bin/bash
set -eo pipefail

# 双模式打包：优先 uv，回退 pip（确保旧镜像回滚兼容）
if command -v uv &>/dev/null && [ -f "pyproject.toml" ]; then
  echo "[pack] uv mode: locking dependencies"
  uv lock
  # 同步生成 requirements.txt 以供旧镜像回滚使用
  echo "[pack] generating requirements.txt for backward compatibility"
  uv export --frozen --no-hashes --no-dev | grep -v "^#" | grep -v "^$" | grep -v "^    " | sed 's/ ;.*//' > requirements.txt
else
  echo "[pack] pip fallback mode: freezing dependencies"
  pip freeze --exclude watchdog > requirements.txt
fi
