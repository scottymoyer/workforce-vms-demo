#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# BENIGN traffic generator.
# Exercises every endpoint with VALID input so that:
#   • APM traces flow for each route,
#   • IAST confirms reachability of each sink (green "code executed" evidence),
#   • runtime SCA sees the vulnerable libraries actually load + execute,
#   • Database Monitoring gets query load.
#
# Usage:  ./scripts/traffic.sh [BASE_URL] [ITERATIONS]
#   BASE_URL     default http://localhost:8080
#   ITERATIONS   default 25
# ─────────────────────────────────────────────────────────────────────────────
set -uo pipefail

BASE="${1:-${BASE_URL:-http://localhost:8080}}"
ITERS="${2:-25}"
c() { curl -sS -o /dev/null -w "  %{http_code}  $1\n" "${@:2}"; }

echo "▶ Benign traffic against ${BASE} (${ITERS} iterations)"

# Log in as a real user so these traces carry usr.id (account attribution).
TOKEN="$(curl -sS -X POST "${BASE}/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice.admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
AUTH=(-H "Authorization: Bearer ${TOKEN}")
if [ -n "${TOKEN}" ]; then echo "  ✓ logged in as alice.admin (usr.id attributed)"; else echo "  ! login failed — continuing anonymously"; fi

for i in $(seq 1 "${ITERS}"); do
  # Read-only directory (safe DB traces / DBM load)
  c "GET /api/vendors"              "${AUTH[@]}" "${BASE}/api/vendors"
  c "GET /api/workers"             "${AUTH[@]}" "${BASE}/api/workers"
  c "GET /api/timesheets"          "${AUTH[@]}" "${BASE}/api/timesheets"
  c "GET /api/approvals"           "${AUTH[@]}" "${BASE}/api/approvals"
  c "GET /api/vendors/1/billing-status" "${AUTH[@]}" "${BASE}/api/vendors/1/billing-status"

  # SQLi endpoints WITH VALID INPUT (proves reachability; no exploitation)
  c "GET /api/workers/search (valid)"    "${AUTH[@]}" "${BASE}/api/workers/search?name=Ada"
  c "GET /api/timesheets/search (valid)" "${AUTH[@]}" "${BASE}/api/timesheets/search?status=SUBMITTED"

  # Other sinks with benign input
  c "GET /api/status"              "${AUTH[@]}" -H "X-Api-Version: 2.3.1" "${BASE}/api/status"
  c "GET /api/greeting (valid)"    "${AUTH[@]}" "${BASE}/api/greeting?name=Grace"
  c "GET /api/lookup (valid)"      "${AUTH[@]}" --get --data-urlencode "query=Hello from the VMS" "${BASE}/api/lookup"
  c "GET /api/documents (valid)"   "${AUTH[@]}" "${BASE}/api/documents/download?file=contract-1001.txt"
  c "POST /api/workers/register"   "${AUTH[@]}" -X POST --data "name=Jane Doe&title=Analyst&vendorName=Apex Staffing Group" "${BASE}/api/workers/register"
  c "POST /api/vendors/import-yaml (valid)" "${AUTH[@]}" -X POST -H "Content-Type: text/plain" \
      --data $'name: New Vendor\nstatus: ACTIVE' "${BASE}/api/vendors/import-yaml"

  sleep 0.3
done

echo "▶ Done. Check Datadog: APM → Traces (service:workforce-vms), Security → Code Security."
