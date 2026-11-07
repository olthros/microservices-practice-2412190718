#!/bin/bash

set -u

fail=0

check() {
    local name="$1"
    local url="$2"
    code=$(curl -s -L -o /dev/null -w "%{http_code}" --max-time 5 "$url")
    echo "$name -> HTTP $code  ($url)"
    case "$code" in
        2*|3*) ;;
        *) echo "  !! $name 检查失败"; fail=1 ;;
    esac
}

echo "=== 1. Nacos 控制台（本机映射端口 18080）==="
check "nacos-console" "http://localhost:18080/"

echo "=== 2. 各业务服务健康接口 ==="
check "user-service"    "http://localhost:8081/actuator/health"
check "course-service"  "http://localhost:8082/actuator/health"
check "booking-service" "http://localhost:8083/actuator/health"

if [ "$fail" -ne 0 ]; then
    echo "=== 冒烟检查未通过 ==="
    exit 1
fi
echo "=== 全部检查通过 ==="