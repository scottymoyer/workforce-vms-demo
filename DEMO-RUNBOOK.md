# DEMO-RUNBOOK.md — running the workforce-vms demo

Step-by-step for driving the live demo. For the full vulnerability map and the
four-scene narrative, see [README.md](README.md); for the two set-piece remediation
scenarios, see [MITIGATION.md](MITIGATION.md).

> **Two things that trip people up:** run `colima start` before anything Docker, and
> run `traffic.sh` (benign) **before** `attack.sh` (malicious) so reachability/traces
> populate before the App & API Protection signals land.

---

## 0. Prep (once, at the start of a session)

```bash
colima start                                  # Colima does NOT auto-start on reboot
cd /Users/scott.moyer/Projects/workforce-vms-demo
docker ps >/dev/null && echo "daemon OK"      # sanity check
```

## 1. Bring up the stack

```bash
docker compose up --build -d                  # app + postgres + datadog-agent
```

Wait ~15s, then confirm the app is healthy:

```bash
curl -s localhost:8080/actuator/health        # → {"status":"UP"}
```

To watch it boot instead: `docker compose logs -f app` — look for
`Started WorkforceVmsApplication` and `iast_enabled: FULLY_ENABLED`.

## 2. Sanity-check a couple of endpoints

```bash
curl -s -X POST localhost:8080/login -H 'Content-Type: application/json' \
  -d '{"username":"alice.admin","password":"admin123"}'          # → JWT

curl -s "localhost:8080/api/workers/search?name=Ada"             # → 1 worker
```

## 3. Generate traffic — benign first, then attack

```bash
./scripts/traffic.sh          # benign: traces, IAST reachability, SCA load
./scripts/attack.sh           # malicious: SQLi/XSS/traversal/Log4Shell/etc. → AAP signals
```

Give it ~2–3 min for data to surface in Datadog.

## 4. Where to look in Datadog (the four scenes)

| Scene | Datadog UI | What to show |
|---|---|---|
| 1. SCA + reachability | Security → Code Security → **Vulnerabilities** (SCA), filter `service:workforce-vms` | Full CVE list; sort by **reachable / code-executed** → prioritize |
| 2. SAST + IDE/PR | Security → Code Security → **Static Analysis** (+ the Datadog IDE plugin inline) | First-party findings on the `// DEMO-VULN:` lines |
| 3. IAST runtime (SQLi) | Security → Code Security → **Vulnerabilities** (IAST) + **APM → Traces** | Tainted input → SQL sink with file:line; the trace shows the SQL **and** the input param + `usr.id` |
| 4. App & API Protection | **App and API Protection → Signals** | Live attacks with source IP, endpoint, and `usr.id` (account attribution) |

## 5. The two set-piece scenarios (detail in MITIGATION.md)

**Log4Shell — non-upgrade mitigating control:**
1. Uncomment `LOG4J_FORMAT_MSG_NO_LOOKUPS: "true"` in `docker-compose.yml` (app service).
2. `docker compose up -d --build app`
3. Re-run the Log4Shell request:
   ```bash
   curl -s -H 'X-Api-Version: ${jndi:ldap://x/y}' localhost:8080/api/status
   ```
   → payload is now logged literally, no JNDI lookup. Mark the finding *Mitigated* in
   Datadog and/or show the AAP virtual-patch WAF rule.

**SnakeYAML — upgrade blast radius (no Docker needed, pure "watch it break"):**
```bash
sed -i '' 's#<snakeyaml.version>1.30</snakeyaml.version>#<snakeyaml.version>2.0</snakeyaml.version>#' pom.xml
# rebuild → LegacyConfigParser fails to COMPILE (Constructor(Class) removed in 2.0)
git checkout pom.xml     # revert when done
```

## 6. Reset / teardown

```bash
docker compose restart app        # clean app state mid-demo
docker compose down               # stop everything (keeps postgres volume + images)
docker compose down -v            # full clean incl. seeded DB
colima stop                       # shut the VM when fully done
```

## Finding SAST results in your IDE (VS Code / Cursor)

The Datadog extension runs the static analyzer locally using the rulesets in
`code-security.datadog.yaml`, so first-party findings appear **as you edit**:

1. **Datadog panel** — the Datadog icon in the left **Activity Bar** opens a
   *Code Security / Vulnerabilities* view listing findings for the workspace.
2. **Inline squiggles** — the offending line (each `// DEMO-VULN:` line) gets a colored
   underline; hovering shows the rule (e.g. `java-security/sql-injection`), severity, and
   a quick-fix / "learn more" link.
3. **Problems panel** — `View → Problems` (⇧⌘M) lists every finding with file:line;
   clicking jumps to the code.

**Cursor note:** Cursor is a VS Code fork but defaults to the **Open VSX** marketplace,
where the Datadog extension may not be published. If it doesn't show in the Extensions
search, install the VSIX manually: download `datadog-vscode` from the VS Code Marketplace
(or Datadog's releases), then in Cursor run **Extensions: Install from VSIX…** from the
command palette (⇧⌘P). After installing, sign in to Datadog from the extension so it can
pull SCA/IAST findings back from the platform. Local SAST (the inline squiggles) works
from the bundled analyzer + `code-security.datadog.yaml` without a sign-in.

## Quick reference

- **App:** http://localhost:8080
- **Users:** `alice.admin/admin123` · `bob.approver/approve123` · `carol.recruiter/recruit123` · `dave.vendor/vendor123`
- **Full endpoint → vuln → Datadog-surface map + narrative:** `README.md` §2 and §4
- **App won't start?** `docker compose logs app` — usually Colima not started, or port 8080 in use.
