#!/bin/bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail=0

check() {
    local name="$1"
    local url="$2"
    code=$(curl -s -L -o /dev/null -w "%{http_code}" --max-time 5 "$url")
    echo "$name -> HTTP $code  ($url)"
    case "$code" in
        2*|3*) ;;
        *) echo "  !! $name check failed"; fail=1 ;;
    esac
}

echo "=== 1. Nacos console ==="
check "nacos-console" "http://localhost:18080/"

echo "=== 2. gateway and services health ==="
check "gateway-service" "http://localhost:8080/api/health"
check "user-service"    "http://localhost:8081/actuator/health"
check "booking-service" "http://localhost:8083/actuator/health"

COURSE_ADDRESS="$(docker compose port --index=1 course-service 8082 2>/dev/null | head -n 1)"
COURSE_ADDRESS="${COURSE_ADDRESS/0.0.0.0/127.0.0.1}"

if [ -n "$COURSE_ADDRESS" ]; then
    check "course-service" "http://$COURSE_ADDRESS/actuator/health"
else
    echo "course-service -> could not resolve host port"
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    echo "=== smoke test failed ==="
    exit 1
fi
echo "=== all checks passed ==="
