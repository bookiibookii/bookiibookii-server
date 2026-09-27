#!/usr/bin/env bash
# 볼륨 테스트(단일 요청) 일괄 실행: 후보 API를 VU 1명으로 순서대로 측정하고 결과를 CSV로 모은다.
# 사용: STAGE=S bash performance/volume/run-volume.sh   (일부만: ONLY='^groups' STAGE=L-index ...)
set -euo pipefail

cd "$(dirname "$0")"
STAGE="${STAGE:?STAGE (S|M|L|after) is required}"
DURATION="${DURATION:-30s}"
HEAVY_USER="${HEAVY_USER:-1}"
NORMAL_USER="${NORMAL_USER:-100000}"
OUT_DIR="results/${STAGE}"
mkdir -p "$OUT_DIR"

HEAVY_TOKEN=$(python mint-token.py "$HEAVY_USER")
NORMAL_TOKEN=$(python mint-token.py "$NORMAL_USER")

# 형식: <라벨> <TARGET> <토큰 종류> [NICKNAME]
CASES=(
  "groups-list groups-list heavy"
  "groups-list-region groups-list-region heavy"
  "groups-search groups-search heavy"
  "groups-home groups-home heavy"
  "bookshelf-heavy bookshelf heavy"
  "bookshelf-normal bookshelf normal"
  "profile-heavy profile heavy reader${HEAVY_USER}"
  "profile-bookshelf-heavy profile-bookshelf heavy reader${HEAVY_USER}"
  "me-trackers-heavy me-trackers heavy"
  "me-trackers-normal me-trackers normal"
  "tracker-detail tracker-detail heavy"
  "library-heavy library heavy"
  "library-search-heavy library-search heavy"
  "notifications-heavy notifications heavy"
  "reviews-written-heavy reviews-written heavy"
  "reviews-received-heavy reviews-received heavy"
)

CSV="$OUT_DIR/summary.csv"
echo "stage,case,requests,fail_rate,avg_ms,med_ms,p95_ms,p99_ms,max_ms" > "$CSV"

for c in "${CASES[@]}"; do
  read -r label target kind nick <<< "$c"
  # ONLY: 측정할 케이스 라벨 정규식 (예: ONLY='^groups|^profile')
  if [ -n "${ONLY:-}" ] && ! [[ "$label" =~ $ONLY ]]; then continue; fi
  token=$([ "$kind" = heavy ] && echo "$HEAVY_TOKEN" || echo "$NORMAL_TOKEN")

  # 워밍업(JIT, 커넥션, 버퍼 풀) 후 측정
  MSYS_NO_PATHCONV=1 k6 run --quiet -e TOKEN="$token" -e TARGET="$target" -e NICKNAME="${nick:-reader1}" \
    -e MODE=single -e DURATION=5s volume-test.js > /dev/null 2>&1 || true

  MSYS_NO_PATHCONV=1 k6 run --quiet -e TOKEN="$token" -e TARGET="$target" -e NICKNAME="${nick:-reader1}" \
    -e MODE=single -e DURATION="$DURATION" --summary-export="$OUT_DIR/$label.json" volume-test.js \
    > "$OUT_DIR/$label.log" 2>&1 || true

  python - "$OUT_DIR/$label.json" "$STAGE" "$label" >> "$CSV" <<'EOF'
import json, sys
m = json.load(open(sys.argv[1]))["metrics"]
d = m["http_req_duration"]
f = m.get("http_req_failed", {}).get("value", 0)
print(f'{sys.argv[2]},{sys.argv[3]},{m["http_reqs"]["count"]},{f:.4f},'
      f'{d["avg"]:.1f},{d["med"]:.1f},{d["p(95)"]:.1f},{d["p(99)"]:.1f},{d["max"]:.1f}')
EOF
  tail -1 "$CSV"
done

echo "done → $CSV"
