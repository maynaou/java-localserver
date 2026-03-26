#!/bin/bash
# ======================================
# 🧪 Full QA Script for JavaServer
# ======================================

SERVER_IP="127.0.0.1"
SERVER_PORT="8080"
BASE_URL="http://$SERVER_IP:$SERVER_PORT"
HOST_HEADER="test.com"
BODY_LIMIT=1048576   # 1MB
TEST_LOG="full_audit.log"

echo "=============================="
echo "🚀 START FULL AUDIT"
echo "==============================" | tee -a $TEST_LOG

# ── Basic GET request ─────────────────────────
echo "➡️ Basic GET request" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" $BASE_URL | tee -a $TEST_LOG

# ── Virtual hosting test ─────────────────────
echo "➡️ Virtual hosting" | tee -a $TEST_LOG
curl -s -H "Host: $HOST_HEADER" $BASE_URL | tee -a $TEST_LOG

# ── 404 / 403 / 413 tests ────────────────────
echo "➡️ 404 test" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" $BASE_URL/nonexistent | tee -a $TEST_LOG

echo "➡️ 403 test" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" $BASE_URL/../secret | tee -a $TEST_LOG

echo "➡️ 413 test" | tee -a $TEST_LOG
dd if=/dev/zero bs=1 count=$((BODY_LIMIT + 1000)) 2>/dev/null | \
curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE_URL --data-binary @- | tee -a $TEST_LOG

# ── Redirection test ─────────────────────────
echo "➡️ Redirection test" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" -L $BASE_URL/redirect | tee -a $TEST_LOG

# ── Keep-alive test ─────────────────────────
echo "➡️ Keep-alive test" | tee -a $TEST_LOG
curl -s -D - -o /dev/null -H "Connection: keep-alive" $BASE_URL | grep "Connection:" | tee -a $TEST_LOG

# ── HTTP methods tests ──────────────────────
echo "➡️ GET / POST / DELETE methods" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" $BASE_URL/index.html | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" -X POST --data "test=1" $BASE_URL/post | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $BASE_URL/delete | tee -a $TEST_LOG

# ── Wrong requests (malformed) ─────────────
echo "➡️ Wrong request test" | tee -a $TEST_LOG
curl -s -o /dev/null -w "%{http_code}\n" -X FOOBAR $BASE_URL | tee -a $TEST_LOG

# ── File upload test ────────────────────────
echo "➡️ File upload / download test" | tee -a $TEST_LOG
echo "Hello World" > test_upload.txt
curl -s -X POST --data-binary @test_upload.txt $BASE_URL/upload | tee -a $TEST_LOG
curl -s $BASE_URL/upload/test_upload.txt | tee -a $TEST_LOG
rm test_upload.txt

# ── Stress test using siege ─────────────────
if command -v siege >/dev/null 2>&1; then
    echo "➡️ Stress test 30s, 100 clients" | tee -a $TEST_LOG
    siege -b -t30S -c100 $BASE_URL | tee -a $TEST_LOG
else
    echo "⚠️ Siege not found, skipping stress tests" | tee -a $TEST_LOG
fi

# ── Memory check ───────────────────────────
echo "➡️ Memory check" | tee -a $TEST_LOG
PID_LIST=$(pgrep -f 'Main')
for pid in $PID_LIST; do
    ps -p $pid -o pid,comm,%mem,rss,vsz | tee -a $TEST_LOG
done

echo "=============================="
echo "✅ FULL AUDIT FINISHED"
echo "==============================" | tee -a $TEST_LOG