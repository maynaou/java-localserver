#!/usr/bin/env bash

PASS=0
FAIL=0

pass() { echo "[PASS] $1"; PASS=$((PASS + 1)); }
fail() { echo "[FAIL] $1"; FAIL=$((FAIL + 1)); }

echo "======================================"
echo "  Java HTTP Server Tests"
echo "======================================"

# ── 1. Fichiers statiques ─────────────────
echo ""
echo "=== Static Files ==="

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/)
[ "$HTTP_CODE" = "200" ] && pass "GET / returned 200" || fail "GET / expected 200, got $HTTP_CODE"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/index.html)
[ "$HTTP_CODE" = "200" ] && pass "GET /index.html returned 200" || fail "GET /index.html expected 200, got $HTTP_CODE"

# ── 2. Error pages ────────────────────────
echo ""
echo "=== Error Pages ==="

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/inexistant)
[ "$HTTP_CODE" = "404" ] && pass "GET /inexistant returned 404" || fail "GET /inexistant expected 404, got $HTTP_CODE"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/)
[ "$HTTP_CODE" = "405" ] && pass "DELETE / returned 405" || fail "DELETE / expected 405, got $HTTP_CODE"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/)
[ "$HTTP_CODE" = "405" ] && pass "PUT / returned 405" || fail "PUT / expected 405, got $HTTP_CODE"

# ── 3. Path traversal ────────────────────
echo ""
echo "=== Security ==="

HTTP_CODE=$(curl --path-as-is -s -o /dev/null -w "%{http_code}" http://localhost:8080/../config.json)
[ "$HTTP_CODE" = "403" ] && pass "path traversal returned 403" || fail "path traversal expected 403, got $HTTP_CODE"

# ── 4. Upload ────────────────────────────
echo ""
echo "=== Upload ==="

echo "test content" > /tmp/test_upload.txt
HTTP_CODE=$(curl -s -o /tmp/upload_response.txt -w "%{http_code}" \
    -X POST -F "file=@/tmp/test_upload.txt" http://localhost:8080/upload)
[ "$HTTP_CODE" = "201" ] && pass "POST /upload returned 201" || fail "POST /upload expected 201, got $HTTP_CODE"

# ── 5. Delete ────────────────────────────
echo ""
echo "=== Delete ==="

echo "to delete" > /tmp/test_delete.txt
curl -s -X POST -F "file=@/tmp/test_delete.txt;filename=test_delete.txt" \
    http://localhost:8080/upload > /dev/null

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    http://localhost:8080/upload/test_delete.txt)
[ "$HTTP_CODE" = "200" ] && pass "DELETE /upload/test_delete.txt returned 200" || fail "DELETE expected 200, got $HTTP_CODE"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE \
    http://localhost:8080/upload/test_delete.txt)
[ "$HTTP_CODE" = "404" ] && pass "DELETE inexistant returned 404" || fail "DELETE inexistant expected 404, got $HTTP_CODE"

# ── 6. Body size limit ───────────────────
echo ""
echo "=== Body Size Limit ==="

dd if=/dev/zero of=/tmp/bigfile.dat bs=1M count=12 status=none
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    --data-binary @/tmp/bigfile.dat http://localhost:8080/upload)
[ "$HTTP_CODE" = "413" ] && pass "oversized upload returned 413" || fail "oversized upload expected 413, got $HTTP_CODE"
rm -f /tmp/bigfile.dat

# ── 7. Redirect ──────────────────────────
echo ""
echo "=== Redirect ==="

RESPONSE=$(curl -s -D - -o /dev/null http://localhost:8080/old-page)
echo "$RESPONSE" | grep -q "301" && pass "GET /old-page returned 301" || fail "GET /old-page expected 301"
echo "$RESPONSE" | grep -qi "Location: /new-page" && pass "redirect Location is /new-page" || fail "redirect Location missing"

# ── 8. Directory listing ─────────────────
echo ""
echo "=== Directory Listing ==="

mkdir -p www/files
echo "hello" > www/files/file1.txt
HTTP_CODE=$(curl -s -o /tmp/listing.html -w "%{http_code}" http://localhost:8080/files)
[ "$HTTP_CODE" = "200" ] && pass "GET /files returned 200" || fail "GET /files expected 200, got $HTTP_CODE"
grep -q "Index of" /tmp/listing.html && pass "directory listing shows Index of" || fail "directory listing missing"

# ── 9. CGI ───────────────────────────────
echo ""
echo "=== CGI ==="

HTTP_CODE=$(curl -s -o /tmp/cgi.body -w "%{http_code}" http://localhost:8080/scripts/hello.py)
[ "$HTTP_CODE" = "200" ] && pass "GET /scripts/hello.py returned 200" || fail "GET /scripts/hello.py expected 200, got $HTTP_CODE"

# ── 10. Chunked ──────────────────────────
echo ""
echo "=== Chunked ==="

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Transfer-Encoding: chunked" \
    -H "Content-Type: text/plain" \
    --data-binary "Hello Chunked" http://localhost:8080/upload)
[ "$HTTP_CODE" = "201" ] && pass "chunked POST returned 201" || fail "chunked POST expected 201, got $HTTP_CODE"

# ── 11. Multi-ports ──────────────────────
echo ""
echo "=== Multi-ports ==="

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/)
[ "$HTTP_CODE" = "200" ] && pass "port 8080 works" || fail "port 8080 failed"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/)
[ "$HTTP_CODE" = "200" ] && pass "port 8081 works" || fail "port 8081 failed"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9090/)
[ "$HTTP_CODE" = "200" ] && pass "port 9090 works" || fail "port 9090 failed"

# ── 12. Cookies & Sessions ───────────────
echo ""
echo "=== Cookies and Sessions ==="

RESPONSE=$(curl -s -D - http://localhost:8080/)
echo "$RESPONSE" | grep -qi "Set-Cookie: SID=" && pass "SID cookie is set" || fail "SID cookie missing"

SID=$(echo "$RESPONSE" | grep -i "Set-Cookie: SID=" | sed 's/.*SID=\([^;]*\).*/\1/' | tr -d '\r')
RESPONSE2=$(curl -s -D - --cookie "SID=$SID" http://localhost:8080/)
echo "$RESPONSE2" | grep -qi "Set-Cookie: SID=$SID" && pass "same SID reused" || fail "SID not reused"

# ── 13. Timeout ──────────────────────────
echo ""
echo "=== Timeout ==="

echo "Testing timeout (wait 35 seconds)..."
START=$(date +%s)
nc localhost 8080 &
NC_PID=$!
sleep 35
if ! kill -0 $NC_PID 2>/dev/null; then
    pass "inactive connection timed out"
else
    fail "connection not timed out after 35 seconds"
    kill $NC_PID 2>/dev/null
fi

# ── 14. Bad request ──────────────────────
echo ""
echo "=== Bad Request ==="

RESPONSE=$(printf "BROKEN_REQUEST\r\n\r\n" | nc localhost 8080)
echo "$RESPONSE" | grep -q "400" && pass "malformed request returned 400" || fail "malformed request expected 400"

# ✅ Fix — utiliser curl au lieu de nc pour Content-Length invalide
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Content-Length: not-a-number" \
    -H "Content-Type: text/plain" \
    --data "test" \
    http://localhost:8080/upload)
[ "$HTTP_CODE" = "400" ] && pass "invalid Content-Length returned 400" || fail "invalid Content-Length expected 400, got $HTTP_CODE"

# ── Summary ──────────────────────────────
echo ""
echo "======================================"
echo "PASSED: $PASS"
echo "FAILED: $FAIL"
echo "======================================"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1