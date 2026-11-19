#!/usr/bin/env bash

set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-root}"
MEMBER_USERNAME="${MEMBER_USERNAME:-member}"
MEMBER_PASSWORD="${MEMBER_PASSWORD:-root}"

PASS=0
FAIL=0
LAST_CODE=""
LAST_BODY=""

pass() {
    PASS=$((PASS + 1))
    echo "[PASS] $1"
}

fail() {
    FAIL=$((FAIL + 1))
    echo "[FAIL] $1"
}

request() {
    local method="$1"
    local path="$2"
    local data="${3:-}"
    local token="${4:-}"
    local extra_header="${5:-}"
    local response

    local args=(-sS --max-time 10 -X "$method" -H 'Accept: application/json')

    if [[ -n "$data" ]]; then
        args+=(-H 'Content-Type: application/json' -d "$data")
    fi
    if [[ -n "$token" ]]; then
        args+=(-H "Authorization: Bearer $token")
    fi
    if [[ -n "$extra_header" ]]; then
        args+=(-H "$extra_header")
    fi

    if ! response="$(curl "${args[@]}" -w $'\n%{http_code}' "$GATEWAY_URL$path" 2>/dev/null)"; then
        LAST_CODE="000"
        LAST_BODY=""
        return
    fi

    LAST_CODE="${response##*$'\n'}"
    LAST_BODY="${response%$'\n'*}"
}

expect_code() {
    local name="$1"
    local expected="$2"

    if [[ "$LAST_CODE" == "$expected" ]]; then
        pass "$name (HTTP $LAST_CODE)"
    else
        fail "$name (expected HTTP $expected, got HTTP $LAST_CODE)"
        echo "       response: $LAST_BODY"
    fi
}

json_string_field() {
    printf '%s' "$1" |
        sed -n 's/.*"'"$2"'":"\([^"]*\)".*/\1/p'
}

echo "=================================================="
echo " week-09 auth and gateway test"
echo " gateway: $GATEWAY_URL"
echo "=================================================="

echo
echo "=== 0. gateway health ==="
for ((attempt = 1; attempt <= 30; attempt++)); do
    request GET "/api/health"
    if [[ "$LAST_CODE" == "200" ]]; then
        break
    fi
    sleep 2
done
expect_code "GET /api/health" "200"

echo
echo "=== 1. unauthenticated access ==="
request GET "/api/admin/members"
expect_code "GET /api/admin/members without token" "401"

request GET "/api/admin/members" "" "" "X-User-Role: ADMIN"
expect_code "forged X-User-Role without token" "401"

request GET "/api/admin/members" "" "" "X-User-Id: 1"
expect_code "forged X-User-Id without token" "401"

echo
echo "=== 2. wrong password ==="
request POST "/api/auth/login" \
    "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"wrong-password\"}"
expect_code "login with wrong password" "401"

echo
echo "=== 3. admin login ==="
request POST "/api/auth/login" \
    "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}"
expect_code "admin login" "200"

ADMIN_TOKEN="$(json_string_field "$LAST_BODY" accessToken)"

if [[ -n "$ADMIN_TOKEN" ]]; then
    pass "admin login returns accessToken"
else
    fail "admin login has no accessToken"
    echo "       response: $LAST_BODY"
fi

request GET "/api/admin/members" "" "$ADMIN_TOKEN"
expect_code "admin token accesses /api/admin/members" "200"

request GET "/api/auth/me" "" "$ADMIN_TOKEN"
expect_code "admin token accesses /api/auth/me" "200"

echo
echo "=== 4. member login and authorization ==="
request POST "/api/auth/login" \
    "{\"username\":\"$MEMBER_USERNAME\",\"password\":\"$MEMBER_PASSWORD\"}"
expect_code "member login" "200"

MEMBER_TOKEN="$(json_string_field "$LAST_BODY" accessToken)"

if [[ -n "$MEMBER_TOKEN" ]]; then
    pass "member login returns accessToken"
else
    fail "member login has no accessToken"
fi

request GET "/api/members/1/check" "" "$MEMBER_TOKEN"
expect_code "member token accesses /api/members/1/check" "200"

request GET "/api/members/1/check"
expect_code "GET /api/members/1/check without token" "401"

request GET "/api/admin/members" "" "$MEMBER_TOKEN"
expect_code "member token accesses /api/admin/members" "403"

echo
echo "=== 5. tampered token ==="
TAMPERED_TOKEN="${ADMIN_TOKEN%??}xx"
request GET "/api/admin/members" "" "$TAMPERED_TOKEN"
expect_code "tampered token" "401"

echo
echo "=================================================="
echo " passed: $PASS, failed: $FAIL"
echo "=================================================="

if [[ "$FAIL" -ne 0 ]]; then
    echo "AUTH TEST FAILED"
    exit 1
fi

echo "AUTH TEST PASSED"
