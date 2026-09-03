# workforce-vms — Datadog Code Security demo (contingent-workforce / VMS)

A deliberately-vulnerable **Java 17 / Spring Boot** monolith on **PostgreSQL**, lightly
themed as a **Vendor Management System** (Vendors, Workers, Timesheets, Approvals,
Users). It runs with a single `docker compose up` and immediately generates Datadog
telemetry across the whole SDLC:

```
 IDE  ───────────►  CI  ───────────►  CD  ───────────►  Runtime
 SAST inline       SAST + SCA        Quality Gate      IAST + runtime SCA + AAP
 (Datadog plugin)  SBOM + git meta   → gated deploy    (dd-java-agent)
```

> ⚠️ **This app is intentionally insecure.** Every planted flaw is tagged `// DEMO-VULN:`
> in source and mapped in the table below. Run it only locally, for demos.

---

## 1. One-command setup

**Prereqs:** Docker Engine + Docker Compose v2, and a Datadog **sandbox** API key.

> **No Docker Desktop license?** Use [Colima](https://github.com/abiosoft/colima) (the
> sanctioned Datadog workaround). One-time setup:
> ```bash
> brew install colima docker
> colima start --cpu 4 --memory 6 --disk 30   # sets the docker context to "colima"
> ```
> The `docker` CLI + `docker compose` then work unchanged. This demo has been verified
> end-to-end on Colima.

```bash
cp .env.example .env
#   edit .env → set DD_API_KEY (and DD_SITE if not US1)
docker compose up --build
```

That's it. Three containers come up: `app` (under `dd-java-agent`), `postgres`, and
`datadog-agent`. The app listens on **http://localhost:8080**.

Optional — link findings to file/line/PR (Source Code Integration), set before build:
```bash
export DD_GIT_REPOSITORY_URL="github.com/your-org/workforce-vms-demo"
export DD_GIT_COMMIT_SHA="$(git rev-parse HEAD)"
docker compose up --build
```

### Seed users (for `/login` + account attribution)

| Username | Password | Role |
|----------|----------|------|
| `alice.admin` | `admin123` | ADMIN |
| `bob.approver` | `approve123` | APPROVER |
| `carol.recruiter` | `recruit123` | RECRUITER |
| `dave.vendor` | `vendor123` | VENDOR |

```bash
curl -X POST http://localhost:8080/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice.admin","password":"admin123"}'
# → { "token": "...", "username": "alice.admin", "role": "ADMIN" }
```
Send that token as `Authorization: Bearer <token>` and every trace + security signal
carries `usr.id` (see [DatadogUserTracking.java](src/main/java/com/workforce/vms/security/DatadogUserTracking.java)).

---

## 2. Vulnerability map

Each vuln is **reachable from an HTTP endpoint** so Datadog can confirm the code/library
is not just present but *executed*.

### Open-source (SCA)

| # | Vuln | CVE / version | Endpoint that reaches it | Datadog surface | SDLC |
|---|------|---------------|--------------------------|-----------------|------|
| 1 | Spring4Shell | CVE-2022-22965 · spring-beans **5.3.17** | `POST /api/workers/register` (POJO data binding) | SCA · AAP | CI · Runtime |
| 2 | Log4Shell | CVE-2021-44228 · log4j-core **2.14.1** | `GET /api/status` header `X-Api-Version` | SCA · AAP | CI · Runtime |
| 3 | Text4Shell | CVE-2022-42889 · commons-text **1.9** | `GET /api/lookup?query=` | SCA · AAP | CI · Runtime |
| 4 | SnakeYAML deserialization | CVE-2022-1471 · snakeyaml **1.30** | `POST /api/vendors/import-yaml` | SCA · IAST | CI · Runtime |
| 5 | Embedded Tomcat | e.g. CVE-2022-23181 · tomcat-embed **9.0.58** | any HTTP request (servlet container) | SCA | CI · Runtime |
| 6 | jackson-databind | CVE-2020-36518 · **2.13.1** | any JSON endpoint | SCA | CI · Runtime |
| 7 | Guava | CVE-2018-10237 / CVE-2020-8908 · **24.1.1-jre** | transitive (loaded on boot) | SCA | CI · Runtime |
| 8 | commons-collections | CVE-2015-6420 · **3.2.1** | `POST /api/admin/restore-config` (gadget) | SCA · IAST | CI · Runtime |

### First-party (SAST + IAST)

| # | Vuln | Type | Endpoint | Datadog surface | SDLC |
|---|------|------|----------|-----------------|------|
| 9 | **SQL injection (centerpiece)** | CWE-89 | `GET /api/workers/search?name=` · `GET /api/timesheets/search?status=` | SAST · **IAST** · APM trace | IDE · CI · Runtime |
| 10 | Command injection | CWE-78 | `GET /api/admin/ping?host=` | SAST · IAST · AAP | IDE · CI · Runtime |
| 11 | Path traversal | CWE-22 | `GET /api/documents/download?file=` | SAST · IAST · AAP | IDE · CI · Runtime |
| 12 | Reflected XSS | CWE-79 | `GET /api/greeting?name=` | SAST · IAST · AAP | IDE · CI · Runtime |
| 13 | SSRF | CWE-918 | `GET /api/vendors/logo?url=` | SAST · IAST · AAP | IDE · CI · Runtime |
| 14 | Weak crypto (MD5) | CWE-327 | `POST /login` (via `PasswordHasher`) | SAST | IDE · CI |
| 15 | Hardcoded secret | CWE-798 | `DemoSecrets` · `GET /api/vendors/{id}/billing-status` | SAST | IDE · CI |
| 16 | Insecure deserialization | CWE-502 | `POST /api/admin/restore-config` | SAST · IAST | IDE · CI · Runtime |

> The two "hard" remediation stories (non-upgrade mitigating control for Log4Shell, and
> the SnakeYAML upgrade blast radius) live in **[MITIGATION.md](MITIGATION.md)**.

---

## 3. SDLC checkpoints

| Checkpoint | What's wired | Where |
|------------|--------------|-------|
| **IDE** | Datadog VS Code plugin recommended; reads `code-security.datadog.yaml` (java-security ruleset) → SAST findings inline as you type | [.vscode/extensions.json](.vscode/extensions.json) |
| **CI** | Datadog Static Analysis (SAST), SCA via CycloneDX SBOM upload, git-metadata upload, build+test, container image scan, **Quality Gate** | [.github/workflows/ci.yml](.github/workflows/ci.yml) |
| **CD** | Deploy job gated on the CI Quality Gate (`workflow_run` + `conclusion == success`); ECR/Fargate slot documented | [.github/workflows/cd.yml](.github/workflows/cd.yml) |
| **Runtime** | `dd-java-agent` with APM + IAST (`DD_IAST_ENABLED`) + runtime SCA (`DD_APPSEC_SCA_ENABLED`) + App & API Protection (`DD_APPSEC_ENABLED`) | [docker-compose.yml](docker-compose.yml) |

---

## 4. Demo flow (four scenes + two special scenarios)

Bring the stack up, then generate traffic:

```bash
./scripts/traffic.sh          # BENIGN — drives traces, IAST reachability, SCA load
./scripts/attack.sh           # MALICIOUS — drives AAP Signals + attribution
```

### Scene 1 — SCA + reachability prioritization
Run `traffic.sh`. In **Security → Code Security → Vulnerabilities** (or **Software
Composition Analysis**), filter to `service:workforce-vms`. You'll see the full library
CVE list (Log4Shell, Spring4Shell, Text4Shell, SnakeYAML, Tomcat, jackson, guava,
commons-collections). **Sort by "Code executed / reachable"** — the handful that
`traffic.sh` actually exercised bubble to the top. *Story: volume vs. what's reachable;
prioritize the reachable ones.*

### Scene 2 — SAST on first-party code + PR/IDE remediation
Open the repo in VS Code with the Datadog plugin → SQLi, command injection, weak MD5,
hardcoded secret etc. flag **inline** on the exact `// DEMO-VULN:` lines. The same
findings appear in CI (**Static Analysis** job) and as **in-PR comments** via Source
Code Integration. *Story: shift-left; developer fixes before merge; the PR Gate can
block.*

### Scene 3 — IAST runtime exploitability (SQLi + attribution)
This is the centerpiece. `attack.sh` hits `GET /api/workers/search?name=' OR '1'='1`
**while logged in as `bob.approver`**.
- **Security → Code Security → Vulnerabilities** shows an **IAST** SQL-injection finding
  with the **tainted input → SQL sink** flow and the **exact file/line**
  ([WorkerSearchController.java](src/main/java/com/workforce/vms/controller/WorkerSearchController.java)).
- **APM → Traces** for that request shows the **executed SQL statement with the injected
  input visible** on the `postgres` db span — the "see the input parameter in the trace"
  ask — plus `usr.id: bob.approver` on the trace. *Story: not theoretical — here's the
  exploit, the line of code, and whose account did it.*

### Scene 4 — App & API Protection: live attacks + API inventory + blocking
`attack.sh` fires SQLi, XSS, path traversal, command injection, the Log4Shell header,
Text4Shell, SnakeYAML, an SSRF at `169.254.169.254`, and `sqlmap`/`Nikto` user-agents.
- **App and API Protection → Signals** lights up with attacker source IP, matched rule,
  targeted endpoint, and `usr.id` (account attribution).
- **API Catalog / Inventory** populates from observed traffic.
- **Blocking demo:** add `bob.approver` (or the source IP) to a blocklist, or enable the
  custom WAF rule from MITIGATION.md, then re-run `attack.sh` → requests return **403
  Blocked**.

### Special scenario A — non-upgrade mitigating control (Log4Shell)
See **[MITIGATION.md § Scenario 1](MITIGATION.md)**: neutralize CVE-2021-44228 without a
version bump via `LOG4J_FORMAT_MSG_NO_LOOKUPS` / `%m{nolookups}` **and** an AAP virtual
patch, then mark the finding *Mitigated* in Datadog.

### Special scenario B — upgrade blast radius (SnakeYAML)
See **[MITIGATION.md § Scenario 2](MITIGATION.md)**: bumping snakeyaml 1.30 → 2.0 to fix
CVE-2022-1471 **breaks `LegacyConfigParser` at compile time**; the CI build/test +
Quality Gate catch it pre-merge, and a coding agent can scope the blast radius.

---

## 5. Where each finding shows up in Datadog

| Product | UI location | Populated by |
|---------|-------------|--------------|
| SCA (libraries) | Security → Code Security → **Vulnerabilities** (SCA filter) | CI SBOM upload + runtime `DD_APPSEC_SCA_ENABLED` |
| SAST (first-party) | Security → Code Security → **Vulnerabilities** (Static Analysis) | CI static-analysis job + IDE plugin |
| IAST (runtime) | Security → Code Security → **Vulnerabilities** (Runtime/IAST) | `traffic.sh` + `attack.sh` under `DD_IAST_ENABLED` |
| App & API Protection | **App and API Protection → Signals / Traces** | `attack.sh` under `DD_APPSEC_ENABLED` |
| APM (SQL + input param) | **APM → Traces** (`service:workforce-vms`) | any traffic; db span shows the statement |

---

## 6. IDE plugin + Datadog MCP server (optional)

- **Datadog VS Code plugin** — install `Datadog.datadog-vscode` (VS Code will prompt from
  `.vscode/extensions.json`). It reads `code-security.datadog.yaml` and shows SAST
  findings inline, plus SCA/IAST findings pulled back from Datadog.
- **Datadog MCP server** — connect it to a coding agent (Claude / Copilot / Bits AI) so
  the agent can pull the live findings for this service, open the offending file/line,
  and propose fixes (including scoping the SnakeYAML blast radius in MITIGATION.md).

---

## 7. Cleanup

```bash
docker compose down -v      # stop + remove the postgres volume
```

---

## 8. Project layout

```
pom.xml                       # pinned VULNERABLE dependency versions (real CVEs)
src/main/java/com/workforce/vms/
  controller/                 # one reachable endpoint per planted vuln (// DEMO-VULN:)
  security/                   # JWT, MD5 hasher, Datadog usr.id attribution
  legacy/LegacyConfigParser   # SnakeYAML upgrade blast-radius module
  config/DemoSecrets          # hardcoded secret
src/main/resources/           # application.yml, log4j2.xml, schema.sql, data.sql
Dockerfile                    # multi-stage; downloads dd-java-agent.jar
docker-compose.yml            # app + postgres + datadog-agent
.env.example                  # DD_API_KEY, DD_SITE, DD_ENV, DD_SERVICE, DD_VERSION
code-security.datadog.yaml    # Datadog SAST ruleset (current format)
static-analysis.datadog.yml   # legacy SAST ruleset (spec/back-compat)
.github/workflows/ci.yml      # SAST + SCA + quality gate + build/scan
.github/workflows/cd.yml      # deploy, gated by the quality gate
scripts/traffic.sh            # BENIGN traffic
scripts/attack.sh             # MALICIOUS payloads
.vscode/extensions.json       # recommends the Datadog IDE plugin
README.md                     # this runbook
MITIGATION.md                 # mitigating control + upgrade blast-radius
```
