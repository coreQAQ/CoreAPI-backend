#!/usr/bin/env bash
set -euo pipefail

# -----------------------------------------------------------------------------
# 配置区
# -----------------------------------------------------------------------------
ES_URL="${ES_URL:-http://localhost:9200}"
ES_USER="${ES_USER:-elastic}"       # 默认 elastic
ES_PASS="${ELASTIC_PASSWORD}"
MARKER_FILE="/usr/share/elasticsearch/.mapping_initialized"

AUTH=""
if [[ -n "$ES_USER" && -n "$ES_PASS" ]]; then
  AUTH="-u $ES_USER:$ES_PASS"
fi

# -----------------------------------------------------------------------------
# 1. 后台启动 Elasticsearch
# -----------------------------------------------------------------------------
echo "[INFO] Starting Elasticsearch in background ..."
/usr/local/bin/docker-entrypoint.sh eswrapper &

# -----------------------------------------------------------------------------
# 2. 等待 ES 启动，端口通+集群健康
# -----------------------------------------------------------------------------
echo "[INFO] Waiting for Elasticsearch to be ready at $ES_URL ..."
until curl -s $AUTH "$ES_URL" >/dev/null 2>&1; do
  sleep 1
done

# 再等集群健康（最多 60s）
if ! curl -s $AUTH "$ES_URL/_cluster/health?wait_for_status=yellow&timeout=60s" | grep -q '"status"'; then
  echo "[ERROR] ES cluster health check failed"
  exit 1
fi
echo "[INFO] Elasticsearch is up!"

# -----------------------------------------------------------------------------
# 3. 初始化 mapping（只执行一次）
# -----------------------------------------------------------------------------
if [[ -f "$MARKER_FILE" ]]; then
  echo "[INFO] Mapping already initialized once (marker file present). Skipping."
else
  echo "[INFO] Initializing indices from /usr/share/elasticsearch/mapping/*.json ..."
  shopt -s nullglob
  for file in /usr/share/elasticsearch/mapping/*.json; do
    index="$(basename "$file" .json)"
    # 判断索引是否存在
    if curl -s $AUTH -o /dev/null -w '%{http_code}' "$ES_URL/$index" | grep -qE '^(200|301)$'; then
      echo "[WARN] Index '$index' already exists. Skip creation."
      continue
    fi
    echo "[INFO] Creating index '$index' ..."
    if curl -s $AUTH -o /dev/null -w '%{http_code}' \
         -XPUT "$ES_URL/$index" \
         -H 'Content-Type: application/json' \
         --data-binary "@$file" | grep -q '^2'; then
      echo "[INFO] Index '$index' created successfully."
    else
      echo "[ERROR] Failed to create index '$index'."
    fi
  done
  touch "$MARKER_FILE"
  echo "[INFO] Mapping initialization completed."
fi

# -----------------------------------------------------------------------------
# 4. 前台等待 ES 主进程，保持容器存活
# -----------------------------------------------------------------------------
echo "[INFO] Handing over control to Elasticsearch main process ..."
wait -n
