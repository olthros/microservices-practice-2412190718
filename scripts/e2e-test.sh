#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BOOKING_URL="${BOOKING_URL:-http://localhost:8083}"
USER_URL="${USER_URL:-http://localhost:8081}"
USER_STOPPED=0

fail() {
    echo "[FAIL] $*" >&2
    exit 1
}

command -v curl >/dev/null 2>&1 || fail "未找到 curl，请先安装 curl"
command -v docker >/dev/null 2>&1 || fail "未找到 docker，请先启动 Docker Desktop"
[[ -f compose.yaml ]] || fail "请从仓库根目录运行本脚本"

json_number_field() {
    local json="$1"
    local field="$2"
    printf '%s' "$json" |
        sed -n 's/.*"'"$field"'":\([0-9][0-9]*\).*/\1/p'
}

json_string_field() {
    local json="$1"
    local field="$2"
    printf '%s' "$json" |
        sed -n 's/.*"'"$field"'":"\([^"]*\)".*/\1/p'
}

request() {
    local method="$1"
    local url="$2"
    local data="${3:-}"
    local response

    if [[ -n "$data" ]]; then
        response="$(curl -sS --max-time 10 -w $'\n%{http_code}' \
            -X "$method" \
            -H 'Content-Type: application/json' \
            -d "$data" \
            "$url")"
    else
        response="$(curl -sS --max-time 10 -w $'\n%{http_code}' \
            -X "$method" \
            "$url")"
    fi

    HTTP_CODE="$(printf '%s\n' "$response" | sed -n '$p')"
    HTTP_BODY="$(printf '%s\n' "$response" | sed '$d')"
}

wait_for_health() {
    local name="$1"
    local url="$2"
    local code

    for ((attempt = 1; attempt <= 30; attempt++)); do
        code="$(curl -s \
            -o /dev/null \
            -w '%{http_code}' \
            --max-time 2 \
            "$url" || true)"

        if [[ "$code" == "200" ]]; then
            echo "[OK] $name 健康检查通过：$url"
            return 0
        fi

        sleep 2
    done

    fail "$name 健康检查超时：$url"
}

restore_user_service() {
    if [[ "$USER_STOPPED" == "1" ]]; then
        echo "[恢复] 正在重新启动 user-service..."
        docker compose start user-service >/dev/null || true
        wait_for_health \
            "user-service" \
            "$USER_URL/actuator/health" || true
    fi
}

trap restore_user_service EXIT

post_booking_until_success() {
    local member_id="$1"
    local course_id="$2"

    for ((attempt = 1; attempt <= 15; attempt++)); do
        request POST \
            "$BOOKING_URL/api/book?memberId=$member_id&courseId=$course_id"

        if [[ "$HTTP_CODE" == "200" &&
              "$HTTP_BODY" == *'"status":"SUCCESS"'* ]]; then
            return 0
        fi

        # 只有明确提示“预约未执行”的读取失败才等待服务注册后重试。
        # 写超时或其他写调用结果未知时直接失败，避免重复预约。
        if [[ "$HTTP_CODE" != "503" ||
              "$HTTP_BODY" != *"预约未执行"* ]]; then
            break
        fi

        sleep 2
    done

    echo "最后一次响应：HTTP $HTTP_CODE $HTTP_BODY" >&2
    fail "预约请求未成功"
}

wait_for_health \
    "booking-service" \
    "$BOOKING_URL/actuator/health"

wait_for_health \
    "user-service" \
    "$USER_URL/actuator/health"

COURSE_ADDRESS="$(
    docker compose port --index=1 course-service 8082
)"

[[ -n "$COURSE_ADDRESS" ]] ||
    fail "无法获取 course-service 的宿主机映射端口"

COURSE_ADDRESS="${COURSE_ADDRESS/0.0.0.0/127.0.0.1}"
COURSE_URL="http://$COURSE_ADDRESS"

wait_for_health \
    "course-service" \
    "$COURSE_URL/actuator/health"

echo
echo "=== 1. 准备全新的测试会员和课程 ==="

request POST "$USER_URL/api/members" \
    '{"name":"e2e-member","remainingSessions":30}'

[[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]] ||
    fail "创建会员失败：HTTP $HTTP_CODE $HTTP_BODY"

MEMBER_ID="$(json_number_field "$HTTP_BODY" id)"

[[ "$MEMBER_ID" =~ ^[0-9]+$ ]] ||
    fail "会员响应中没有有效的 id：$HTTP_BODY"

echo "memberId=$MEMBER_ID"

request POST "$COURSE_URL/api/courses" \
    '{"name":"e2e-yoga","capacity":100,"bookedCount":0}'

[[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" ]] ||
    fail "创建课程失败：HTTP $HTTP_CODE $HTTP_BODY"

COURSE_ID="$(json_number_field "$HTTP_BODY" id)"

[[ "$COURSE_ID" =~ ^[0-9]+$ ]] ||
    fail "课程响应中没有有效的 id：$HTTP_BODY"

echo "courseId=$COURSE_ID"

echo
echo "=== 2. 连续发起 8 次真实预约，收集课程实例 ID ==="

declare -A COURSE_INSTANCES=()

for ((index = 1; index <= 8; index++)); do
    post_booking_until_success "$MEMBER_ID" "$COURSE_ID"

    CHECK_INSTANCE_ID="$(
        json_string_field "$HTTP_BODY" courseCheckInstanceId
    )"

    BOOK_INSTANCE_ID="$(
        json_string_field "$HTTP_BODY" courseInstanceId
    )"

    [[ -n "$CHECK_INSTANCE_ID" && -n "$BOOK_INSTANCE_ID" ]] ||
        fail "预约响应缺少课程实例 ID：$HTTP_BODY"

    COURSE_INSTANCES["$CHECK_INSTANCE_ID"]=1
    COURSE_INSTANCES["$BOOK_INSTANCE_ID"]=1

    BOOKED_COUNT="$(
        json_number_field "$HTTP_BODY" bookedCount
    )"

    echo "第 $index 次：HTTP 200，check=$CHECK_INSTANCE_ID，book=$BOOK_INSTANCE_ID，bookedCount=$BOOKED_COUNT"
done

INSTANCE_COUNT="${#COURSE_INSTANCES[@]}"

[[ "$INSTANCE_COUNT" -ge 2 ]] ||
    fail "只观察到 $INSTANCE_COUNT 个课程实例，未证明负载均衡"

echo "[OK] 已实际访问 $INSTANCE_COUNT 个 course-service 实例：${!COURSE_INSTANCES[@]}"

BOOKED_COUNT_BEFORE_FAILURE="$(
    json_number_field "$HTTP_BODY" bookedCount
)"

echo
echo "=== 3. 模拟 user-service 不可用 ==="

docker compose stop user-service >/dev/null
USER_STOPPED=1

sleep 5

request POST \
    "$BOOKING_URL/api/book?memberId=$MEMBER_ID&courseId=$COURSE_ID"

echo "依赖不可用响应：HTTP $HTTP_CODE $HTTP_BODY"

[[ "$HTTP_CODE" == "503" ]] ||
    fail "依赖不可用时预期 HTTP 503，实际为 HTTP $HTTP_CODE"

[[ "$HTTP_BODY" != *'"status":"SUCCESS"'* ]] ||
    fail "依赖不可用却被当成预约成功"

request GET \
    "$COURSE_URL/api/courses/$COURSE_ID/check"

[[ "$HTTP_CODE" == "200" ]] ||
    fail "失败后查询课程状态失败"

BOOKED_COUNT_AFTER_FAILURE="$(
    json_number_field "$HTTP_BODY" bookedCount
)"

[[ "$BOOKED_COUNT_AFTER_FAILURE" == "$BOOKED_COUNT_BEFORE_FAILURE" ]] ||
    fail "user-service 失败后课程预约人数发生变化"

echo "[OK] 依赖不可用返回 503，没有返回业务成功，且 bookedCount 未增加"

docker compose start user-service >/dev/null

wait_for_health \
    "user-service" \
    "$USER_URL/actuator/health"

USER_STOPPED=0
trap - EXIT

echo
echo "=== 4. 验证服务恢复 ==="

post_booking_until_success "$MEMBER_ID" "$COURSE_ID"

echo "[OK] user-service 恢复后预约成功：$HTTP_BODY"

echo
echo "=== E2E TEST PASSED ==="
echo "跨服务成功、负载均衡（$INSTANCE_COUNT 个实例）、依赖失败和服务恢复均已验证。"
