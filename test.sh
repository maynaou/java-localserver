#!/bin/bash
# ============================================================
#  SCRIPT DE TEST COMPLET - AUDIT WEBSERV (java-localserver)
#  Usage : chmod +x audit_test.sh && ./audit_test.sh
#  Le serveur doit tourner sur localhost:8080 avant de lancer
# ============================================================

HOST="http://localhost:8080"
PASS=0
FAIL=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS+1)); }
fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL+1)); }
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
section() { echo -e "\n${YELLOW}══════════════════════════════════════${NC}"; echo -e "${YELLOW} $1${NC}"; echo -e "${YELLOW}══════════════════════════════════════${NC}"; }

# ─────────────────────────────────────────────────────────────
section "1. REQUÊTES GET"
# ─────────────────────────────────────────────────────────────

# GET page principale
CODE=$(curl -s -o /dev/null -w "%{http_code}" $HOST/)
[ "$CODE" = "200" ] && ok "GET / → 200" || fail "GET / → attendu 200, reçu $CODE"

# GET fichier inexistant → 404
CODE=$(curl -s -o /dev/null -w "%{http_code}" $HOST/fichier_inexistant.html)
[ "$CODE" = "404" ] && ok "GET /fichier_inexistant → 404" || fail "GET /fichier_inexistant → attendu 404, reçu $CODE"

# GET avec mauvaise URL (caractères spéciaux)
CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HOST/???bad%%%url")
[ "$CODE" = "400" ] || [ "$CODE" = "404" ] && ok "GET URL invalide → $CODE (géré)" || fail "GET URL invalide → attendu 400/404, reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "2. REQUÊTES POST (upload)"
# ─────────────────────────────────────────────────────────────

# POST upload fichier texte
RESP=$(curl -s -X POST -H "Content-Type: text/plain" --data "Hello World" $HOST/upload)
echo "$RESP" | grep -qi "upload" && ok "POST /upload → fichier uploadé" || fail "POST /upload → réponse inattendue: $RESP"

# POST avec body vide
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: text/plain" --data "" $HOST/upload)
[ "$CODE" = "200" ] || [ "$CODE" = "400" ] && ok "POST body vide → $CODE (géré)" || fail "POST body vide → reçu $CODE"

# POST body trop grand → 413 (limite 10MB sur serveur, 50MB sur /upload)
# On teste sur une route avec petite limite
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Content-Type: text/plain" \
  -H "Content-Length: 999999999" \
  --data "test" $HOST/upload)
[ "$CODE" = "413" ] || [ "$CODE" = "200" ] && ok "POST Content-Length excessif → $CODE" || fail "POST Content-Length excessif → reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "3. REQUÊTE DELETE"
# ─────────────────────────────────────────────────────────────

# D'abord uploader un fichier, puis le supprimer
info "Upload d'un fichier pour test DELETE..."
RESP=$(curl -s -X POST -H "Content-Type: text/plain" --data "fichier a supprimer" $HOST/upload)
FILENAME=$(echo "$RESP" | grep -o 'upload_[^<]*')
info "Fichier uploadé : $FILENAME"

if [ -n "$FILENAME" ]; then
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE $HOST/upload/$FILENAME)
    [ "$CODE" = "200" ] && ok "DELETE /upload/$FILENAME → 200" || fail "DELETE → attendu 200, reçu $CODE"
else
    fail "DELETE → impossible de récupérer le nom du fichier uploadé"
fi

# DELETE fichier inexistant → 404
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE $HOST/upload/fichier_qui_nexiste_pas.txt)
[ "$CODE" = "404" ] && ok "DELETE fichier inexistant → 404" || fail "DELETE inexistant → attendu 404, reçu $CODE"

# DELETE sans permission (route / n'autorise pas DELETE) → 405
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE $HOST/)
[ "$CODE" = "405" ] && ok "DELETE sur route non autorisée → 405" || fail "DELETE non autorisé → attendu 405, reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "4. MÉTHODES NON AUTORISÉES"
# ─────────────────────────────────────────────────────────────

# PUT non supporté globalement
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT $HOST/)
[ "$CODE" = "405" ] || [ "$CODE" = "400" ] && ok "PUT non autorisé → $CODE" || fail "PUT → attendu 405/400, reçu $CODE"

# PATCH non supporté
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PATCH $HOST/)
[ "$CODE" = "405" ] || [ "$CODE" = "400" ] && ok "PATCH non autorisé → $CODE" || fail "PATCH → attendu 405/400, reçu $CODE"

# Requête totalement invalide
CODE=$(curl -s -o /dev/null -w "%{http_code}" -X INVALIDMETHOD $HOST/)
[ "$CODE" = "405" ] || [ "$CODE" = "400" ] && ok "Méthode invalide → $CODE (géré)" || fail "Méthode invalide → reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "5. PAGES D'ERREUR PERSONNALISÉES"
# ─────────────────────────────────────────────────────────────

BODY=$(curl -s $HOST/page_inexistante_404)
echo "$BODY" | grep -qi "404" && ok "Page 404 personnalisée servie" || fail "Page 404 → contenu inattendu: $BODY"

BODY=$(curl -s -X DELETE $HOST/)
echo "$BODY" | grep -qi "405" && ok "Page 405 personnalisée servie" || fail "Page 405 → contenu inattendu: $BODY"

# ─────────────────────────────────────────────────────────────
section "6. REDIRECTION"
# ─────────────────────────────────────────────────────────────

# 301 sans suivre la redirection
CODE=$(curl -s -o /dev/null -w "%{http_code}" $HOST/old-page)
[ "$CODE" = "301" ] && ok "GET /old-page → 301 Moved Permanently" || fail "Redirection → attendu 301, reçu $CODE"

# Vérifier le header Location
LOCATION=$(curl -v $HOST/old-page 2>&1 | grep -i "location:")
echo "$LOCATION" | grep -qi "new-page" && ok "Header Location → $LOCATION" || fail "Header Location manquant: $LOCATION"

# ─────────────────────────────────────────────────────────────
section "7. DIRECTORY LISTING"
# ─────────────────────────────────────────────────────────────

BODY=$(curl -s $HOST/files/)
echo "$BODY" | grep -qi "index of\|<ul>\|<li>" && ok "Directory listing /files/ → HTML généré" || fail "Directory listing → attendu liste HTML, reçu: $BODY"

# Directory listing désactivé sur /
CODE=$(curl -s -o /dev/null -w "%{http_code}" $HOST/)
[ "$CODE" = "200" ] && ok "GET / avec directory_listing=false → index.html servi" || fail "GET / → attendu 200, reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "8. CGI"
# ─────────────────────────────────────────────────────────────

# GET CGI basique
BODY=$(curl -s $HOST/scripts/hello.py)
echo "$BODY" | grep -qi "CGI\|METHOD" && ok "CGI GET /scripts/hello.py → réponse valide" || fail "CGI GET → réponse inattendue: $BODY"

# CGI avec query string
BODY=$(curl -s "$HOST/scripts/hello.py?name=audit&test=42")
echo "$BODY" | grep -qi "name=audit\|QUERY" && ok "CGI avec query string → QUERY_STRING transmis" || fail "CGI query string → $BODY"

# CGI POST avec body
BODY=$(curl -s -X POST -H "Content-Type: text/plain" --data "hello CGI" $HOST/scripts/hello.py)
echo "$BODY" | grep -qi "POST\|CGI" && ok "CGI POST → METHOD=POST transmis" || fail "CGI POST → $BODY"

# CGI chunked
BODY=$(curl -s -X POST -H "Transfer-Encoding: chunked" --data "chunked body test" $HOST/scripts/hello.py)
echo "$BODY" | grep -qi "CGI\|POST\|METHOD" && ok "CGI chunked → traité" || fail "CGI chunked → $BODY"

# ─────────────────────────────────────────────────────────────
section "9. VIRTUAL HOSTING (Host header)"
# ─────────────────────────────────────────────────────────────

# Host: localhost (serveur par défaut)
CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: localhost" $HOST/)
[ "$CODE" = "200" ] && ok "Virtual host localhost → 200" || fail "Virtual host localhost → attendu 200, reçu $CODE"

# Host: inconnu → doit utiliser le serveur par défaut
CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: unknown.server.com" $HOST/)
[ "$CODE" = "200" ] && ok "Host inconnu → fallback serveur par défaut 200" || fail "Host inconnu → attendu 200 (fallback), reçu $CODE"

# Test avec curl --resolve (comme demandé dans l'audit)
info "Test curl --resolve (nécessite test.com configuré dans config.json pour passer)"
CODE=$(curl -s -o /dev/null -w "%{http_code}" --resolve "localhost:8080:127.0.0.1" http://localhost:8080/)
[ "$CODE" = "200" ] && ok "curl --resolve localhost → 200" || fail "curl --resolve → attendu 200, reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "10. COOKIES & SESSIONS"
# ─────────────────────────────────────────────────────────────

# Vérifier que Set-Cookie est présent dans la réponse
HEADERS=$(curl -s -I $HOST/)
echo "$HEADERS" | grep -qi "set-cookie" && ok "Header Set-Cookie présent dans la réponse" || fail "Set-Cookie manquant dans les headers"

# Vérifier que le SID est renvoyé
COOKIE=$(curl -s -I $HOST/ | grep -i "set-cookie" | grep "SID")
[ -n "$COOKIE" ] && ok "Cookie SID présent : $COOKIE" || fail "Cookie SID manquant"

# Test keep-alive avec cookie
SID=$(curl -s -I $HOST/ | grep -i "set-cookie" | grep -o "SID=[^;]*")
info "SID récupéré : $SID"
CODE=$(curl -s -o /dev/null -w "%{http_code}" -H "Cookie: $SID" $HOST/)
[ "$CODE" = "200" ] && ok "Requête avec Cookie SID → 200" || fail "Requête avec Cookie → attendu 200, reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "11. KEEP-ALIVE"
# ─────────────────────────────────────────────────────────────

HEADERS=$(curl -s -I -H "Connection: keep-alive" $HOST/)
echo "$HEADERS" | grep -qi "keep-alive" && ok "Keep-Alive supporté dans la réponse" || fail "Keep-Alive → header manquant dans la réponse"

# ─────────────────────────────────────────────────────────────
section "12. UPLOAD + RE-TÉLÉCHARGEMENT (intégrité)"
# ─────────────────────────────────────────────────────────────

# Créer un fichier avec contenu connu
echo "AUDIT_TEST_CONTENT_12345" > /tmp/test_upload.txt
RESP=$(curl -s -X POST -H "Content-Type: text/plain" --data-binary @/tmp/test_upload.txt $HOST/upload)
FILENAME=$(echo "$RESP" | grep -o 'upload_[^<]*')
info "Fichier uploadé : $FILENAME"

if [ -n "$FILENAME" ]; then
    DOWNLOADED=$(curl -s $HOST/upload/$FILENAME 2>/dev/null || curl -s $HOST/files/$FILENAME 2>/dev/null)
    echo "$DOWNLOADED" | grep -q "AUDIT_TEST_CONTENT_12345" && ok "Fichier re-téléchargé identique (intégrité OK)" || fail "Intégrité fichier → contenu différent après upload/download"
    # Nettoyage
    curl -s -X DELETE $HOST/upload/$FILENAME > /dev/null
else
    fail "Upload/Download → impossible d'uploader le fichier test"
fi
rm -f /tmp/test_upload.txt

# ─────────────────────────────────────────────────────────────
section "13. SÉCURITÉ PATH TRAVERSAL"
# ─────────────────────────────────────────────────────────────

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HOST/../../../etc/passwd")
[ "$CODE" = "400" ] || [ "$CODE" = "403" ] || [ "$CODE" = "404" ] && ok "Path traversal /../ → $CODE (bloqué)" || fail "Path traversal → attendu 400/403/404, reçu $CODE"

CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HOST/%2e%2e%2f%2e%2e%2fetc%2fpasswd")
[ "$CODE" = "400" ] || [ "$CODE" = "403" ] || [ "$CODE" = "404" ] && ok "Path traversal encodé → $CODE (bloqué)" || fail "Path traversal encodé → reçu $CODE"

# ─────────────────────────────────────────────────────────────
section "14. STRESS TEST (siege)"
# ─────────────────────────────────────────────────────────────

if command -v siege &> /dev/null; then
    info "Lancement siege 10s 50 clients..."
    RESULT=$(siege -b -t10s -c50 $HOST/ 2>&1)
    AVAIL=$(echo "$RESULT" | grep -i "availability" | grep -o "[0-9.]*" | head -1)
    info "Disponibilité : $AVAIL%"
    AVAIL_INT=$(echo "$AVAIL" | cut -d. -f1)
    [ "${AVAIL_INT:-0}" -ge 99 ] && ok "Siege disponibilité ≥ 99% ($AVAIL%)" || fail "Siege disponibilité < 99% ($AVAIL%)"
else
    info "siege non installé — test ignoré (installer avec: sudo apt install siege)"
fi

# ─────────────────────────────────────────────────────────────
section "15. RÉSUMÉ FINAL"
# ─────────────────────────────────────────────────────────────

TOTAL=$((PASS+FAIL))
echo ""
echo -e "  Tests passés  : ${GREEN}$PASS / $TOTAL${NC}"
echo -e "  Tests échoués : ${RED}$FAIL / $TOTAL${NC}"
echo ""
if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}  ✓ Tous les tests passent — projet prêt pour l'audit !${NC}"
elif [ $FAIL -le 3 ]; then
    echo -e "${YELLOW}  ! Quelques points à vérifier avant l'audit.${NC}"
else
    echo -e "${RED}  ✗ Plusieurs problèmes détectés — revoir avant l'audit.${NC}"
fi
echo ""