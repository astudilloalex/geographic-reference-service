# Phase 1 Validation Guide: Read Geographic Catalog API

This is the canonical post-implementation validation guide for Phase 1. Run it from the
repository root on the feature revision being evaluated. It validates the approved read-only
service; it is not an implementation or a catalog-loading procedure for production.

The normative behavior remains in [spec.md](spec.md), the persisted and public read models in
[data-model.md](data-model.md), the HTTP shapes and examples in
[contracts/openapi.yaml](contracts/openapi.yaml), and source and legal controls in
[catalog-source-manifest.md](catalog-source-manifest.md). Do not infer a different field,
relation, route, count, or error shape from the examples below.

## Command Status

The repository currently contains the completed Phase 1 design and a minimal Quarkus
implementation scaffold. Use these labels throughout this guide:

- **Available now**: `spec.md`, `research.md`, `plan.md`, `data-model.md`, the source manifest,
  `contracts/openapi.yaml`, and accepted ADRs 0001/0002; `./gradlew --version`; the basic
  `./gradlew clean build`; the Gradle 9.3.1 wrapper; Java 25 and Quarkus 3.33.2.1 configuration;
  the basic CI workflow; and generated Quarkus Dockerfile scaffolding. The design contract can
  be inspected now, but the scaffold build does not validate implemented behavior.
- **Created or completed during implementation**: custom Gradle test tasks, packaged OpenAPI,
  database bootstrap/finalization scripts, migrations, catalog build outputs, compliant runtime,
  migration, and role-management images, Quadlets, and implementation documentation. Run those commands only
  after their outputs exist. A missing required task or artifact is an incomplete
  implementation, not a reason to skip validation.
- **Created only after legal/source approval**: source-derived V002/V003 SQL, the approved
  derived manifest and approval artifact, catalog revision, and `build/catalog/smoke-fixture.json`.
  Before approval, only schema work and clearly synthetic test fixtures may be used.

Check the current scaffold before implementation:

```bash
./gradlew --version
./gradlew clean build
yq -e '.openapi == "3.1.1" and (.paths | length == 16)' \
  specs/001-read-geographic-catalog/contracts/openapi.yaml
test -s docs/adr/0001-internal-jwt-trust-boundary.md
test -s docs/adr/0002-transactional-database-role-finalization.md
```

Expected now: Gradle reports 9.3.1 and Java 25, the minimal build succeeds, and the design
contract has 16 paths. No claim about runtime routes, PostgreSQL, migrations, catalog security,
or deployment follows from these current-state checks.

## Prerequisites

Install or provide the following before running the post-implementation sections:

- Linux with Bash, Git, `curl`, `jq` 1.6 or later, `yq` 4, `sha256sum`, `psql`, `unzip`, and a
  Java 25 JDK.
- Rootless Podman with Quadlet and user-systemd support. Docker may back Testcontainers in CI,
  but rootless Podman is required for deployment validation.
- Permission to pull PostgreSQL 18 and the implementation-pinned Flyway 12.0.0 base image.
- A Testcontainers-compatible container socket. PostgreSQL integration and migration tests
  must report PostgreSQL major version 18; no H2 or substituted database is acceptable.
- An approved development OIDC issuer and JWKS endpoint for manual smoke tests. Automated
  security tests must instead use an in-process Quarkus OIDC/JWKS test server and generated RSA
  keys and must not depend on a live identity platform.
- Separate local PostgreSQL administrator, migration, and runtime credentials. Use disposable
  local values from a password generator or approved secret store. Never commit them or run
  these commands with shell tracing enabled.
- The exact digest-pinned Gitleaks and Trivy OCI image references. No host-installed scanner is
  accepted. Trivy is both the CycloneDX generator and SBOM parser/scanner; no separate,
  unresolved SBOM-validator selection is permitted.
- Written authorization to pass the legal/source gate below before building, distributing,
  deploying, or promoting any INEC-derived artifact.

Confirm rootless execution and the toolchain:

```bash
test "$(podman info --format '{{.Host.Security.Rootless}}')" = "true"
java -version
./gradlew --version
podman version
psql --version
curl --version
jq --version
yq --version
unzip -v
```

## Legal And Source Gate

This gate precedes source-derived response examples, V002/V003 assembly, migration-image
construction, deployment, and traffic promotion. Planning may retain source titles, URLs,
hashes, aggregate counts, classification rules, and minimal anomaly/exclusion references needed
for deterministic review; that evidence is not a redistributable catalog. Before approval,
implementation and CI may run only schema work and clearly synthetic fixtures. No bulk
source-derived INEC rows, generated catalog SQL, response example, or smoke value may be
generated or committed. INEC-derived distributable artifacts may not be built, deployed, or
promoted until the responsible legal or data-governance owner has recorded written approval for
the intended use.

**Created during implementation and runnable before approval:** prove the pre-approval suite is
synthetic and hermetic:

```bash
./gradlew --no-daemon test postgresIntegrationTest migrationTest \
  -PcatalogFixtureProfile=synthetic \
  -Dtestcontainers.reuse.enable=false
```

Expected: no test reads an INEC source archive, approved derived manifest, source-derived SQL,
or production smoke fixture. Synthetic values are visibly artificial and stay under
`src/test/resources/fixtures/`.

The written approval is an authorized manual release-control decision, not something a shell
`test -n` can prove cryptographically. Before continuing, an authorized release reviewer must
verify the approval's use and scope, decision, approver, and date in the governed evidence system
and record that review in release evidence. Automation verifies only that the approved reference
is present and bound to the generated manifest; it never promotes a self-asserted string into
legal approval.

Before continuing, the release evidence must contain all items required by the
[source manifest](catalog-source-manifest.md): retained source artifacts, verified source and
selected-artifact hashes, Debian LGPL notices and attribution, INEC attribution and policy
evidence, extraction report, derived-record manifest, deterministic validation report,
derived-manifest digest, and the written approval reference. Keep confidential approval
evidence in the approved evidence store rather than this repository.

Set references, not secrets or source contents:

```bash
export ISO_CODES_ARCHIVE="/approved/source-store/iso-codes_4.20.1.orig.tar.xz"
export INEC_ARCHIVE="/approved/source-store/CLASIFICADOR_GEOGRAFICO_2026.zip"
export INEC_APPROVAL_REFERENCE="approval-record-reference"
export CATALOG_EVIDENCE_DIRECTORY="/approved/evidence-store/geographic-reference/initial"

test -r "$ISO_CODES_ARCHIVE"
test -r "$INEC_ARCHIVE"
test -n "$INEC_APPROVAL_REFERENCE"
test -d "$CATALOG_EVIDENCE_DIRECTORY"
```

**Created during implementation and runnable only after written approval:** run the
deterministic source validator. It obtains all expected hashes, mappings, exclusions, counts,
RFC 8785 JCS rules, and revision rules from the source manifest rather than operator overrides.

```bash
./gradlew catalogSourceValidation \
  -PisoCodesArchive="$ISO_CODES_ARCHIVE" \
  -PinecArchive="$INEC_ARCHIVE" \
  -PcatalogEvidenceDirectory="$CATALOG_EVIDENCE_DIRECTORY" \
  -PinecApprovalReference="$INEC_APPROVAL_REFERENCE"
```

Expected: the task succeeds only for the pinned inputs and approved extraction and writes
`build/catalog/catalog-derived-manifest-v1.json`,
`build/catalog/catalog-derived-manifest-v1.approval.json`, the independent validation report,
`build/catalog/catalog-revision.txt`, and the approved
`build/catalog/smoke-fixture.json`. The extractor and independent validator must reproduce the
same concrete digest. A missing approval, notice, artifact, hash match, included record,
required identifier, exclusion, or concrete non-placeholder digest fails the task and blocks
all remaining release steps. Do not silently replace either source.

## Expected Artifacts

The Phase 1 design files already exist. The following are **created, generated, or completed
during implementation** and must exist before full validation. `Dockerfile.jvm` exists in the
current scaffold but must be completed to satisfy the runtime-image requirements. The
source-derived files marked post-approval must not exist before the legal/source gate passes.

```text
src/main/resources/META-INF/openapi.yaml
database/bootstrap/prepare-initial-migrator.sql
database/bootstrap/finalize-runtime-login.sql
database/migration/V001__create_geographic_catalog.sql
database/migration/V002__load_initial_catalog_candidate.sql       # post-approval
database/migration/V003__validate_activate_initial_catalog.sql    # post-approval
src/main/docker/Dockerfile.jvm
src/main/docker/Dockerfile.flyway
src/main/docker/Dockerfile.role-management
deploy/quadlet/geographic-reference-migration.container
deploy/quadlet/geographic-reference-finalization.container
deploy/quadlet/geographic-reference-runtime.container
deploy/quadlet/README.md
build/catalog/catalog-derived-manifest-v1.json                    # post-approval
build/catalog/catalog-derived-manifest-v1.approval.json           # post-approval
build/catalog/validation-report.json                              # post-approval
build/catalog/catalog-revision.txt                                # post-approval
build/catalog/smoke-fixture.json                                  # post-approval
build/quarkus-app/quarkus-run.jar
README.md
docs/architecture/geographic-reference-service-v1.drawio
docs/database/v1-schema.dbml
docs/database/roles-and-privileges.md
docs/database/migration-strategy.md
docs/security/read-access.md
docs/deployment/rootless-quadlet.md
docs/operations/runbook.md
docs/local-development.md
docs/testing.md
```

`data-model.md` is the normative proposed Phase 1 design during planning; it does not silently
supersede the current DBML. Implementation is incomplete until `docs/database/v1-schema.dbml`,
`README.md`, `docs/architecture/geographic-reference-service-v1.drawio`, and every planned
database, security, deployment, operations, local-development, and testing document are updated
in the same change to distinguish proposed and implemented behavior accurately.

The design-time OpenAPI file is canonical. Its source copy and the copy packaged in the JVM
artifact must be byte-for-byte identical. V001 creates both NOLOGIN catalog roles, stable
identity and append-only registry tables, snapshot tables, views, revocations, and exact grants;
V002 loads stable mappings and the candidate; V003 validates and activates it. Immutable
migrations contain no login, password, password hash, or other secret.
The runtime image contains no migration tooling, SQL, JDBC driver, source archive, or elevated
credential. The migration image contains the pinned Flyway runtime and approved SQL but no
application.

Check the artifact set:

```bash
test -s specs/001-read-geographic-catalog/data-model.md
test -s specs/001-read-geographic-catalog/contracts/openapi.yaml
cmp specs/001-read-geographic-catalog/contracts/openapi.yaml \
  src/main/resources/META-INF/openapi.yaml
test -s database/bootstrap/prepare-initial-migrator.sql
test -s database/bootstrap/finalize-runtime-login.sql
test -s database/migration/V001__create_geographic_catalog.sql
test -s database/migration/V002__load_initial_catalog_candidate.sql
test -s database/migration/V003__validate_activate_initial_catalog.sql
test -s src/main/docker/Dockerfile.flyway
test -s src/main/docker/Dockerfile.role-management
test -s deploy/quadlet/geographic-reference-migration.container
test -s deploy/quadlet/geographic-reference-finalization.container
test -s deploy/quadlet/geographic-reference-runtime.container
test -s deploy/quadlet/README.md
test -s build/catalog/catalog-revision.txt
test -s build/catalog/smoke-fixture.json
test -s README.md
test -s docs/architecture/geographic-reference-service-v1.drawio
test -s docs/database/v1-schema.dbml
test -s docs/adr/0001-internal-jwt-trust-boundary.md
test -s docs/adr/0002-transactional-database-role-finalization.md
test -s docs/database/roles-and-privileges.md
test -s docs/database/migration-strategy.md
test -s docs/security/read-access.md
test -s docs/deployment/rootless-quadlet.md
test -s docs/operations/runbook.md
test -s docs/local-development.md
test -s docs/testing.md
```

## Environment

Use placeholders or disposable local values. The commands intentionally define no real
password, private key, client secret, or token.

```bash
set +x

export BASE_URL="http://127.0.0.1:8080"
export DB_HOST="127.0.0.1"
export DB_PORT="55432"
export DB_NAME="geographic_reference"
export DB_CONTAINER="grs-postgres-18"
export POSTGRES_IMAGE="docker.io/library/postgres:18@sha256:d93de42662696f278fb34354b06fdaa90ad7ca3106d6f72fbd01d16da006d2cf"
export RUNTIME_CONTAINER="grs-runtime"
export FLYWAY_HISTORY_SCHEMA="flyway_history"

export POSTGRES_ADMIN_USER="postgres"
export POSTGRES_ADMIN_PASSWORD="local-generated-admin-password"
export MIGRATION_DB_USER="geographic_migrator"
export MIGRATION_DB_PASSWORD="local-generated-migration-password"
export RUNTIME_DB_USER="geographic_runtime_login"
export RUNTIME_DB_PASSWORD="local-generated-runtime-password"

export OIDC_AUTH_SERVER_URL="https://approved-dev-issuer.example.test/realms/internal"
export OIDC_TOKEN_URL="${OIDC_AUTH_SERVER_URL}/protocol/openid-connect/token"
export OIDC_JWKS_URL="${OIDC_AUTH_SERVER_URL}/protocol/openid-connect/certs"
export OIDC_AUDIENCE="geographic-reference-service"

export BUILD_REVISION="$(git rev-parse HEAD)"
export CATALOG_REVISION="$(<build/catalog/catalog-revision.txt)"
export RUNTIME_IMAGE="localhost/geographic-reference-service:${BUILD_REVISION}"
export MIGRATION_IMAGE="localhost/geographic-reference-migration:${BUILD_REVISION}"
export ROLE_MANAGEMENT_IMAGE="localhost/geographic-reference-role-management:${BUILD_REVISION}"
```

The local migration and runtime passwords above are disposable secret inputs, not immutable
migration content. No password or hash appears in V001-V003, an image layer, Git, logs, or
release evidence. Production obtains all login secrets through approved secret management.
Runtime configuration includes only the reactive PostgreSQL runtime identity, OIDC issuer/JWKS
settings, RS256 allowlist, audience, build revision, and expected catalog revision. Flyway uses
the migration login, JDBC URL, and explicitly configured history schema only in the external
migration process.

## Gradle Quality Gates

**Created during implementation:** these task names form the local and CI validation
interface. Run with the committed wrapper only.

```bash
./gradlew --no-daemon clean spotlessCheck check
./gradlew --no-daemon \
  architectureTest reactiveTest oidcSecurityTest openApiContractTest routeInventoryTest \
  packagedOpenApiTest documentationTest
CANARY_LOG="/tmp/grs-blocking-canary.log"
CANARY_REPORT="build/test-results/blockingCanary/TEST-com.alexastudillo.geographicreference.support.BlockedEventLoopCanaryTest.xml"
rm -f "$CANARY_LOG" "$CANARY_REPORT"
if ./gradlew --no-daemon blockingCanary --rerun-tasks >"$CANARY_LOG" 2>&1; then
  echo "known event-loop blocking canary unexpectedly passed" >&2
  exit 1
fi
test -s "$CANARY_REPORT"
grep -Eq '<failure[^>]*message="EXPECTED_BLOCKED_EVENT_LOOP_CANARY: BlockedThreadChecker[^"]*"' \
  "$CANARY_REPORT"
./gradlew --no-daemon \
  postgresIntegrationTest migrationTest runtimePrivilegeTest
./gradlew --no-daemon spotbugsMain quarkusBuild
```

Expected:

- Unit and application tests cover normalization, lifecycle and half-open validity,
  localization fallback, bounded hierarchy, pagination, security and error precedence, and
  reactive failure propagation.
- Architecture and reactive tests reject mutation use cases or repositories, write REST
  annotations, generic CRUD, direct REST-to-database access, runtime JDBC/Flyway, blocking
  calls, and manual subscription. A mandatory JUnit extension converts Vert.x
  `BlockedThreadChecker` warnings for event-loop threads into test failures on the JUnit thread,
  using test-profile thresholds of 200 ms maximum execution and 50 ms warning/sampling. The
  separate one-second blocking canary must fail with a current JUnit XML `<failure>` whose message
  begins `EXPECTED_BLOCKED_EVENT_LOOP_CANARY: BlockedThreadChecker`; stale output is deleted
  first, and an unrelated Gradle, compile, startup, or test failure does not pass this gate.
- Contract and route tests prove exactly the canonical operations, GET/HEAD parity, write and
  application-OPTIONS exclusion, the custom non-blocking pre-security Vert.x `@RouteFilter`,
  authenticated access, stable RFC 9457 problems, ETags, and the production route allowlist.
- Hermetic OIDC tests run an in-process Quarkus OIDC/JWKS server with generated RSA keys. They
  prove RS256, issuer, audience, expiry, optional not-before, subject, exact scope separation,
  `401`/`403` serialization, and pre-conversion access precedence without a network identity
  dependency. They also prove the 30-second initial-key startup bound, five-minute refresh,
  atomic rotation, temporary refresh failure with cached-key validation, one-minute unknown-key
  refresh limiting, empty-key readiness failure, and restart while the issuer is unavailable.
- Instrumented tests prove every database-backed catalog query and database-dependent
  readiness, startup, or info observation uses at most one prepared statement. Pre-database
  outcomes, liveness, and metrics use zero database statements.
- Explicit `asOf`, including a valid future date, uses ordinary interval, lifecycle,
  dependency, and coverage semantics. It is not rejected merely because it is in the future.
- Static analysis and formatting report no violations. `quarkusBuild` creates the JVM fast-jar;
  no native build is part of acceptance.

### PostgreSQL 18 And Testcontainers

Do not set a database-compatibility override or substitute H2. The three database suites must
start the exact digest-pinned PostgreSQL 18 image assigned to `POSTGRES_IMAGE` and assert
`server_version_num` is in the PostgreSQL 18 range.

```bash
./gradlew --no-daemon postgresIntegrationTest \
  --info -Dtestcontainers.reuse.enable=false
./gradlew --no-daemon migrationTest \
  --info -Dtestcontainers.reuse.enable=false
./gradlew --no-daemon runtimePrivilegeTest \
  --info -Dtestcontainers.reuse.enable=false
```

Expected: clean-database migration, deterministic activation, constraints, prepared reactive
queries, ordering, hierarchy bounds, query plans, timeouts, revision mismatch, failure
atomicity, recovery, and every required privilege denial pass against PostgreSQL 18. The
initial feature has no supported prior production revision, so clean creation is mandatory;
upgrade tests become mandatory after the baseline is released. Migration tests also prove the
prepare/V001/V002/V003/finalize role flow, Flyway history-schema isolation, reconnect behavior,
exact `pg_auth_members` admin/inherit/set options, owner `SET ROLE`, removal of one-time
`CREATEROLE`, login-specific runtime defaults, stable registry non-reuse, a version-2 logical
projection digest over 21 sections unaffected by public manifest identity, UUIDs, or execution
metadata, and absence of credentials from immutable SQL.

## Migration And Runtime Separation

**Created during implementation:** inspect the production runtime classpath.

```bash
./gradlew --no-daemon dependencies --configuration runtimeClasspath \
  | tee /tmp/grs-runtime-classpath.txt

! grep -Eiq 'flyway|org\.postgresql:postgresql|testcontainers|archunit|openapi-parser' \
  /tmp/grs-runtime-classpath.txt
grep -q 'quarkus-reactive-pg-client' /tmp/grs-runtime-classpath.txt
```

Expected: reactive PostgreSQL support is present. Flyway, JDBC, PostgreSQL Testcontainers,
OpenAPI parser, architecture-test tooling, and migration-only dependencies are absent from
`runtimeClasspath`. The external migration image alone has JDBC and Flyway.

After image construction, inspect file separation:

```bash
podman build -f src/main/docker/Dockerfile.flyway -t "$MIGRATION_IMAGE" .
podman build -f src/main/docker/Dockerfile.role-management -t "$ROLE_MANAGEMENT_IMAGE" .
podman build -f src/main/docker/Dockerfile.jvm -t "$RUNTIME_IMAGE" .

podman run --rm --entrypoint /bin/sh "$RUNTIME_IMAGE" -c \
  'test ! -d /flyway && test ! -d /database/migration'
podman run --rm --entrypoint /bin/sh "$MIGRATION_IMAGE" -c \
  'test ! -e /deployments/quarkus-run.jar && test ! -d /deployments/app'
podman run --rm --entrypoint /bin/sh "$ROLE_MANAGEMENT_IMAGE" -c \
  'test -f /database/bootstrap/prepare-initial-migrator.sql &&
   test -f /database/bootstrap/finalize-runtime-login.sql &&
   test ! -d /database/migration && test ! -e /deployments/quarkus-run.jar'
```

Expected: no image contains source archives or credential values; the runtime contains no
migration SQL/tooling, the migration image contains no runtime application, and the
role-management image contains only the reviewed bootstrap/finalization scripts rather than
catalog migration SQL or application code. Image scanning later in this guide is still required.

## OpenAPI And Route Inventory

The canonical OpenAPI 3.1.1 design is **available now**. The promoted source resource, packaged
copy, runtime route inventory, and related tests are **created during implementation**.

```bash
export OPENAPI="specs/001-read-geographic-catalog/contracts/openapi.yaml"

cmp "$OPENAPI" src/main/resources/META-INF/openapi.yaml
yq -o=json '.' "$OPENAPI" | jq -e '
  .openapi == "3.1.1" and
  (.paths | length == 16) and
  ([.paths[] | to_entries[] | select(.key | IN("get", "head"))] | length == 32) and
  ([.paths[] | keys[] | select(IN("post", "put", "patch", "delete", "options", "trace"))]
    | length == 0) and
  (.webhooks == null)'
yq -r '.paths | keys[]' "$OPENAPI"
./gradlew --no-daemon quarkusBuild openApiContractTest routeInventoryTest packagedOpenApiTest

PACKAGED_APP_JAR="$(find build/quarkus-app/app -type f -name '*.jar' -print -quit)"
test -n "$PACKAGED_APP_JAR"
unzip -p "$PACKAGED_APP_JAR" META-INF/openapi.yaml > /tmp/grs-packaged-openapi.yaml
cmp "$OPENAPI" /tmp/grs-packaged-openapi.yaml
```

Expected: 11 `/v1` catalog paths and the five approved `/q` operational paths produce exactly
16 paths and 32 explicit GET/HEAD operations. There are no request bodies, callbacks,
webhooks, writes, `OPTIONS`, `TRACE`, generated OpenAPI route, Dev UI route in production, or
additional `/v1` or `/q` route. The packaged bytes equal the design contract. A custom
application-owned, non-blocking Vert.x `@RouteFilter` matches only the exact known templates,
runs before HTTP security and REST routing, passes GET/HEAD, and emits the canonical body and
`Allow: GET, HEAD` for every other recognized method, including automatic `OPTIONS`. Use the
contract, not this guide, for parameters, schemas, examples, headers, and the complete matrix.

## Local Migration And Runtime Startup

The following is a disposable local proof of the production order: cluster bootstrap,
external migration, catalog integrity verification, then runtime startup with only a
SELECT-only credential. It is **created during implementation** except for the generic Podman
and Gradle commands.

Start PostgreSQL 18 on loopback only. The platform preparation script, not the container entry
point, creates the target service database:

```bash
podman run --name "$DB_CONTAINER" -d \
  -p "127.0.0.1:${DB_PORT}:5432" \
  -e POSTGRES_DB="postgres" \
  -e POSTGRES_USER="$POSTGRES_ADMIN_USER" \
  -e POSTGRES_PASSWORD="$POSTGRES_ADMIN_PASSWORD" \
  "$POSTGRES_IMAGE"

until PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" pg_isready \
  -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_ADMIN_USER" -d postgres; do
  sleep 1
done
```

Run the initial platform preparation with the disposable administrator. It creates only the
target database, Flyway history schema, and secret-managed `geographic_migrator` login with the
database/history privileges Flyway needs and temporary `CREATEROLE`. It must not create
`geographic_owner`, `geographic_runtime`, a runtime login, or a catalog object. The migration
password is a secret input to this initial operation; it is never an immutable SQL literal.

```bash
PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" psql \
  -v ON_ERROR_STOP=1 \
  -v database_name="$DB_NAME" \
  -v history_schema="$FLYWAY_HISTORY_SCHEMA" \
  -v migrator_login="$MIGRATION_DB_USER" \
  -v migration_password="$MIGRATION_DB_PASSWORD" \
  -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_ADMIN_USER" -d postgres \
  -f database/bootstrap/prepare-initial-migrator.sql

PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_ADMIN_USER" -d "$DB_NAME" \
  -c "SELECT to_regrole('geographic_owner') IS NULL AS owner_absent,
             to_regrole('geographic_runtime') IS NULL AS runtime_role_absent,
             EXISTS (SELECT FROM pg_namespace WHERE nspname = '$FLYWAY_HISTORY_SCHEMA') AS history_schema_ready,
             (SELECT rolcreaterole FROM pg_roles WHERE rolname = '$MIGRATION_DB_USER')
               AS migrator_temporarily_has_createrole"
```

Expected: all four booleans are true. Run Flyway externally from the implementation image,
whose base is the research-pinned Flyway 12.0.0 image digest. The process receives only the
migration credential. Configure the history schema explicitly so Flyway history remains owned
and written by the migration login while migration SQL brackets owner-level catalog work with
`SET ROLE geographic_owner` and `RESET ROLE`.

```bash
run_flyway() {
  podman run --rm --network host \
    -e FLYWAY_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
    -e FLYWAY_USER="$MIGRATION_DB_USER" \
    -e FLYWAY_PASSWORD="$MIGRATION_DB_PASSWORD" \
    -e FLYWAY_DEFAULT_SCHEMA="$FLYWAY_HISTORY_SCHEMA" \
    -e FLYWAY_SCHEMAS="$FLYWAY_HISTORY_SCHEMA" \
    -e FLYWAY_TABLE="flyway_schema_history" \
    -e FLYWAY_LOCATIONS="filesystem:/flyway/sql" \
    -e FLYWAY_VALIDATE_ON_MIGRATE="true" \
    -e FLYWAY_CLEAN_DISABLED="true" \
    -e FLYWAY_BASELINE_ON_MIGRATE="false" \
    -e FLYWAY_OUT_OF_ORDER="false" \
    -e FLYWAY_MIXED="false" \
    -e FLYWAY_GROUP="true" \
    "$MIGRATION_IMAGE" "$@"
}

run_flyway migrate

PGPASSWORD="$MIGRATION_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$MIGRATION_DB_USER" -d "$DB_NAME" \
  -c "SELECT count(*) = 3 AND bool_and(success) AS grouped_history_complete
        FROM ${FLYWAY_HISTORY_SCHEMA}.flyway_schema_history" \
  -c "SELECT tableowner = '$MIGRATION_DB_USER' AS history_owned_by_migrator
        FROM pg_tables
       WHERE schemaname = '$FLYWAY_HISTORY_SCHEMA'
         AND tablename = 'flyway_schema_history'"

# This is a fresh connection after Flyway. Owner SET works, but temporary automatic creator
# ADMIN rows deliberately remain until the privileged finalization transaction.
PGPASSWORD="$MIGRATION_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$MIGRATION_DB_USER" -d "$DB_NAME" \
  -c 'SET ROLE geographic_owner' \
  -c "SELECT current_user = 'geographic_owner' AS owner_role_active" \
  -c 'RESET ROLE' \
  -c "SELECT parent.rolname AS granted_role, grantor.rolname AS grantor,
             membership.admin_option,
             membership.inherit_option, membership.set_option
        FROM pg_auth_members membership
        JOIN pg_roles member ON member.oid = membership.member
        JOIN pg_roles parent ON parent.oid = membership.roleid
        JOIN pg_roles grantor ON grantor.oid = membership.grantor
       WHERE member.rolname = '$MIGRATION_DB_USER'
         AND parent.rolname IN ('geographic_owner', 'geographic_runtime')
       ORDER BY parent.rolname"
```

Expected: Flyway validates and applies exactly the grouped versioned release. V001 creates
`geographic_owner` and `geographic_runtime` as NOLOGIN roles. PostgreSQL's unchangeable
bootstrap-superuser-granted rows remain temporarily with `admin_option=true`,
`inherit_option=false`, and `set_option=false` for both roles; V001 adds a separate non-admin,
non-inherited owner row with `set_option=true` and no runtime SET/INHERIT row. Runtime is not
allowed to start in this temporary state. V001 creates schemas, objects, active views, PUBLIC
revocations, and exact grants as the owner. V002 loads stable identities and append-only registry
mappings before the approved candidate that references them. V003 validates source/legal
evidence, exact counts and both digests, then atomically switches the singleton active pointer as
its final statement. A failure leaves no partial active catalog. Flyway `clean`, automatic
`repair`, repeatable catalog migrations, and editing an applied migration are not recovery
procedures.

Only after grouped V001-V003 succeeds, run the platform finalization script as administrator.
It creates the secret runtime login, grants the exact non-admin inherited membership only in
`geographic_runtime`, removes all migrator role-admin authority, revokes the migrator's one-time
`CREATEROLE`, transfers the database and Flyway history ownership to `geographic_owner`, retains
only exact history privileges and owner SET membership for the migrator, and applies session
defaults to the concrete runtime login. The script takes a deployment advisory lock, executes
all changes and assertions in one transaction, and accepts only the complete pre-finalization or
already validated final state. Any failure rolls back every role, ownership, grant, and setting
change; after correction the same finalization can be rerun safely. It changes no catalog data
and embeds no credential in an immutable migration.

```bash
PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -v runtime_login="$RUNTIME_DB_USER" \
  -v runtime_password="$RUNTIME_DB_PASSWORD" \
  -v migrator_login="$MIGRATION_DB_USER" \
  -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_ADMIN_USER" -d "$DB_NAME" \
  -f database/bootstrap/finalize-runtime-login.sql

PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$POSTGRES_ADMIN_USER" -d "$DB_NAME" \
  -c "SELECT rolname, rolcanlogin, rolcreaterole
        FROM pg_roles
       WHERE rolname IN ('$MIGRATION_DB_USER', '$RUNTIME_DB_USER',
                         'geographic_owner', 'geographic_runtime')
       ORDER BY rolname" \
  -c "SELECT parent.rolname AS granted_role, member.rolname AS member,
             membership.admin_option, membership.inherit_option, membership.set_option
        FROM pg_auth_members membership
        JOIN pg_roles member ON member.oid = membership.member
        JOIN pg_roles parent ON parent.oid = membership.roleid
       WHERE member.rolname IN ('$MIGRATION_DB_USER', '$RUNTIME_DB_USER')
         AND parent.rolname IN ('geographic_owner', 'geographic_runtime')
       ORDER BY member.rolname, parent.rolname" \
  -c "SELECT pg_has_role('$MIGRATION_DB_USER', 'geographic_owner', 'SET')
             AS migrator_can_set_owner"

# Reconnect after hardening: history access and owner SET membership must still work.
run_flyway validate
PGPASSWORD="$MIGRATION_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$MIGRATION_DB_USER" -d "$DB_NAME" \
  -c 'SET ROLE geographic_owner' \
  -c "SELECT current_user = 'geographic_owner' AS owner_role_active" \
  -c 'RESET ROLE'

runtime_psql() {
  PGPASSWORD="$RUNTIME_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 -At \
    -h "$DB_HOST" -p "$DB_PORT" -U "$RUNTIME_DB_USER" -d "$DB_NAME" "$@"
}

test "$(runtime_psql -c 'SHOW default_transaction_read_only')" = "on"
test "$(runtime_psql -c 'SHOW statement_timeout')" = "5s"
test "$(runtime_psql -c 'SHOW search_path' | tr -d ' ')" = "geographic_api,pg_catalog"
if runtime_psql -c 'SELECT pg_sleep(6)' >/dev/null 2>&1; then
  echo "runtime statement_timeout did not cancel a six-second statement" >&2
  exit 1
fi
```

Expected: owner/runtime roles are NOLOGIN, the runtime login can log in but belongs only to
`geographic_runtime` through exactly one row with `admin_option=false`, `inherit_option=true`,
and `set_option=false`. The migrator has exactly one owner row with `admin_option=false`,
`inherit_option=false`, and `set_option=true`, remains a login with owner SET membership, has no
runtime membership, and has `rolcreaterole=false`. A fresh Flyway connection validates the
history table in `$FLYWAY_HISTORY_SCHEMA`; a fresh `psql` connection can still set/reset the
owner role. Runtime-login connections receive all three exact defaults and cancel the bounded
six-second probe. Runtime startup is blocked until these post-bootstrap assertions pass.

The automated migration/deployment suite must additionally inject a failure after each
finalization stage, prove the transaction returns to the complete temporary pre-finalization
state, rerun successfully, rerun once against the accepted final state, and prove the runtime
Quadlet cannot start while the finalization unit is failed or incomplete. It also starts two
finalizers against the same database and advisory-lock key, proves the second blocks behind the
first, observes one state transition, and verifies that the second invocation accepts the final
state after the first commits.

Start the runtime with the reactive URL and runtime credential only:

```bash
podman run --name "$RUNTIME_CONTAINER" -d --network host \
  -e QUARKUS_DATASOURCE_REACTIVE_URL="postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  -e QUARKUS_DATASOURCE_USERNAME="$RUNTIME_DB_USER" \
  -e QUARKUS_DATASOURCE_PASSWORD="$RUNTIME_DB_PASSWORD" \
  -e QUARKUS_OIDC_AUTH_SERVER_URL="$OIDC_AUTH_SERVER_URL" \
  -e GEOGRAPHIC_OIDC_JWKS_URL="$OIDC_JWKS_URL" \
  -e GEOGRAPHIC_OIDC_AUDIENCE="$OIDC_AUDIENCE" \
  -e GEOGRAPHIC_EXPECTED_CATALOG_REVISION="$CATALOG_REVISION" \
  -e GEOGRAPHIC_BUILD_REVISION="$BUILD_REVISION" \
  "$RUNTIME_IMAGE"
```

Expected: no JDBC URL, Flyway setting, migration username, migration password, source
artifact, or catalog-write capability enters the runtime container. Startup remains down if
configuration, schema compatibility, active catalog completeness, or expected revision is
wrong.

## JWTs

The approved gateway is the only service ingress, but the application still validates bearer
JWT signature, RS256 algorithm, issuer, audience, expiry, optional not-before, and subject.
Permissions are exact space-delimited `scope` values; observe permission does not imply read,
and read does not imply observe.

Automated acceptance is hermetic and must run before manual smoke testing:

```bash
./gradlew --no-daemon oidcSecurityTest
```

Expected: an in-process Quarkus OIDC/JWKS test server and per-test generated RSA keys prove the
complete RS256 claim and scope matrix, key rotation/failure behavior, canonical `401`/`403`
problems, and access-before-input precedence without DNS, Internet, gateway, or live IdP access.

For manual smoke testing only, obtain separate short-lived tokens from the approved development
IdP by client credentials:

```bash
export READ_CLIENT_ID="approved-read-test-client"
export READ_CLIENT_SECRET="secret-from-approved-store"
export OBSERVE_CLIENT_ID="approved-observe-test-client"
export OBSERVE_CLIENT_SECRET="secret-from-approved-store"

READ_TOKEN="$(curl -fsS \
  --user "${READ_CLIENT_ID}:${READ_CLIENT_SECRET}" \
  --data-urlencode 'grant_type=client_credentials' \
  --data-urlencode 'scope=geographic-reference.read' \
  "$OIDC_TOKEN_URL" | jq -er '.access_token')"
OBSERVE_TOKEN="$(curl -fsS \
  --user "${OBSERVE_CLIENT_ID}:${OBSERVE_CLIENT_SECRET}" \
  --data-urlencode 'grant_type=client_credentials' \
  --data-urlencode 'scope=geographic-reference.observe' \
  "$OIDC_TOKEN_URL" | jq -er '.access_token')"
export READ_TOKEN OBSERVE_TOKEN
```

If the identity platform provides pre-issued tokens instead, enter them without echoing or
persisting them:

```bash
read -r -s -p 'Read JWT: ' READ_TOKEN; printf '\n'
read -r -s -p 'Observe JWT: ' OBSERVE_TOKEN; printf '\n'
export READ_TOKEN OBSERVE_TOKEN
```

Do not put tokens in this repository, `.env`, command output, logs, screenshots, or evidence
bundles. An invalid, expired, wrong-audience, wrong-issuer, non-RS256, or subjectless token must
be treated as missing/invalid authentication and return the contract's `401` problem.

Define request headers for the remaining commands:

```bash
AUTH_READ=(-H "Authorization: Bearer ${READ_TOKEN}")
AUTH_OBSERVE=(-H "Authorization: Bearer ${OBSERVE_TOKEN}")
```

## Approved Smoke Fixture

Do not commit source-derived INEC codes, names, or ISO province identifiers into this guide,
scripts, tests, or pre-approval logs. After the legal/source validator succeeds, load only its
approved generated smoke fixture. The validator must verify every selected value belongs to the
same approved derived manifest and catalog revision.

```bash
export SMOKE_FIXTURE="build/catalog/smoke-fixture.json"
test -s "$SMOKE_FIXTURE"

export COUNTRY_ALPHA2="$(jq -er '.country.alpha2Code' "$SMOKE_FIXTURE")"
export COUNTRY_ALPHA3="$(jq -er '.country.alpha3Code' "$SMOKE_FIXTURE")"
export COUNTRY_NUMERIC="$(jq -er '.country.numericCode' "$SMOKE_FIXTURE")"
export COUNTRY_LANGUAGE="$(jq -er '.country.language' "$SMOKE_FIXTURE")"
export COUNTRY_REGIONAL_LANGUAGE="$(jq -er '.country.regionalFallbackLanguage' "$SMOKE_FIXTURE")"
export DEFAULT_FALLBACK_LANGUAGE="$(jq -er '.country.defaultFallbackLanguage' "$SMOKE_FIXTURE")"
export DIVISION_COUNTRY_ALPHA2="$(jq -er '.division.countryAlpha2Code' "$SMOKE_FIXTURE")"
export DIVISION_LANGUAGE="$(jq -er '.division.language' "$SMOKE_FIXTURE")"
export DIVISION_REGIONAL_LANGUAGE="$(jq -er '.division.regionalFallbackLanguage' "$SMOKE_FIXTURE")"
export ROOT_DIVISION_CODE="$(jq -er '.division.rootCanonicalCode' "$SMOKE_FIXTURE")"
export CHILD_DIVISION_CODE="$(jq -er '.division.childCanonicalCode' "$SMOKE_FIXTURE")"
export LEAF_DIVISION_CODE="$(jq -er '.division.leafCanonicalCode' "$SMOKE_FIXTURE")"
export DPA_SCHEME="$(jq -er '.division.dpaSchemeCode' "$SMOKE_FIXTURE")"
export DPA_IDENTIFIER="$(jq -er '.division.dpaIdentifierValue' "$SMOKE_FIXTURE")"
export ISO_PROVINCE_SCHEME="$(jq -er '.division.isoProvinceSchemeCode' "$SMOKE_FIXTURE")"
export ISO_PROVINCE_IDENTIFIER="$(jq -er '.division.isoProvinceIdentifierValue' "$SMOKE_FIXTURE")"
export NO_DIVISION_COUNTRY_ALPHA2="$(jq -er '.negative.countryWithoutDivisionCoverage' "$SMOKE_FIXTURE")"
export ABSENT_COUNTRY_CODE="$(jq -er '.negative.absentCountryCode' "$SMOKE_FIXTURE")"
export ABSENT_DIVISION_CODE="$(jq -er '.negative.absentDivisionCode' "$SMOKE_FIXTURE")"
export ABSENT_DPA_IDENTIFIER="$(jq -er '.negative.absentDpaIdentifierValue' "$SMOKE_FIXTURE")"
export COVERAGE_START="$(jq -er '.dates.coverageStart' "$SMOKE_FIXTURE")"
export BEFORE_COVERAGE="$(jq -er '.dates.beforeCoverage' "$SMOKE_FIXTURE")"
export FUTURE_AS_OF="$(jq -er '.dates.futureAsOf' "$SMOKE_FIXTURE")"
export ISO_PROVINCE_IDENTIFIER_URI="$(jq -rn \
  --arg value "$ISO_PROVINCE_IDENTIFIER" '$value | @uri')"

SMOKE_VALUES=(
  "$COUNTRY_ALPHA2" "$COUNTRY_ALPHA3" "$COUNTRY_NUMERIC" "$COUNTRY_LANGUAGE"
  "$COUNTRY_REGIONAL_LANGUAGE" "$DEFAULT_FALLBACK_LANGUAGE"
  "$DIVISION_COUNTRY_ALPHA2" "$DIVISION_LANGUAGE" "$DIVISION_REGIONAL_LANGUAGE"
  "$ROOT_DIVISION_CODE" "$CHILD_DIVISION_CODE" "$LEAF_DIVISION_CODE"
  "$DPA_SCHEME" "$DPA_IDENTIFIER" "$ISO_PROVINCE_SCHEME" "$ISO_PROVINCE_IDENTIFIER"
  "$NO_DIVISION_COUNTRY_ALPHA2" "$ABSENT_COUNTRY_CODE" "$ABSENT_DIVISION_CODE"
  "$ABSENT_DPA_IDENTIFIER" "$COVERAGE_START" "$BEFORE_COVERAGE" "$FUTURE_AS_OF"
)
for value in "${SMOKE_VALUES[@]}"; do test -n "$value"; done
jq -e --arg revision "$CATALOG_REVISION" '.catalogRevision == $revision' "$SMOKE_FIXTURE"
```

Expected: every command exits successfully, no selected variable is empty, and fixture revision
equals the approved revision used by migration and runtime. Before approval this section and
all source-backed manual catalog smoke scenarios are unavailable; run only synthetic automated
tests.

## Catalog Scenarios

These are representative smoke scenarios. The Gradle contract matrix remains authoritative
and exhaustive.

### Metadata And Counts

```bash
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/catalog" | tee /tmp/grs-catalog.json | jq .
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries?page=1&pageSize=100" \
  > /tmp/grs-countries-1.json
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries?page=2&pageSize=100" \
  > /tmp/grs-countries-2.json
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries?page=3&pageSize=100" \
  > /tmp/grs-countries-3.json
jq -s '[.[].items[]] | length' /tmp/grs-countries-{1,2,3}.json
```

Expected: metadata reports the active immutable revision equal to `$CATALOG_REVISION`, source,
coverage, languages, dates, supported schemes, included levels, explicit exclusions, and all
manifest counts. The three bounded country pages total 249 records in alpha-2 order. The
migration and catalog-source tasks, not ad hoc database edits, prove every other exact count.

### Country And Name Lookup

```bash
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries/$COUNTRY_ALPHA2" | jq .
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries/${COUNTRY_ALPHA3,,}" | jq .
curl -fsS "${AUTH_READ[@]}" "$BASE_URL/v1/countries/$COUNTRY_NUMERIC" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2/names?page=1&pageSize=50&language=$COUNTRY_LANGUAGE&nameType=SHORT" | jq .
```

Expected: all three fixture-provided lookups resolve the same approved country representation
and revision; lowercase alphabetic input normalizes to uppercase, numeric input retains three
digits, and the names query applies literal filters and remains bounded.

### Division Lookup And Hierarchy

All source-backed values below come from the post-approval fixture and are not repeated in this
document.

```bash
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-types?page=1&pageSize=50" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions?page=1&pageSize=50" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$ROOT_DIVISION_CODE" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$ROOT_DIVISION_CODE/children?page=1&pageSize=100" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$CHILD_DIVISION_CODE/children?page=1&pageSize=100" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE/ancestors" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-identifiers/$DPA_SCHEME/$DPA_IDENTIFIER" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-identifiers/$ISO_PROVINCE_SCHEME/$ISO_PROVINCE_IDENTIFIER_URI" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE/names?language=$DIVISION_LANGUAGE&nameType=OFFICIAL" | jq .
```

Expected: types are ordered by level then code; roots and direct children are bounded and
ordered by canonical DPA code; the selected leaf resolves through its canonical and approved
DPA identifier; the approved ISO province identifier resolves its selected province; ancestors
are immediate parent then root and contain at most two items. No endpoint returns an arbitrary
recursive tree or crosses the division-country boundary.

### Localization And History

```bash
# Exact, primary-language, and default country-name selection.
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=$COUNTRY_LANGUAGE" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=$COUNTRY_REGIONAL_LANGUAGE" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=$DEFAULT_FALLBACK_LANGUAGE" | jq .

# Exact, primary-language, and default division-name selection.
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE?language=$DIVISION_LANGUAGE" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE?language=$DIVISION_REGIONAL_LANGUAGE" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE?language=$DEFAULT_FALLBACK_LANGUAGE" | jq .

# Earliest coverage and a valid future date use ordinary temporal semantics.
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?asOf=$COVERAGE_START" | jq .
curl -fsS "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?asOf=$FUTURE_AS_OF" | jq .

# A date before endpoint coverage is rejected.
curl -sS -i "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE?asOf=$BEFORE_COVERAGE"
```

Expected: item presentation uses exact tag, primary tag, then default-name fallback. Name-list
`language` remains a literal filter rather than a fallback request. Explicit-`asOf` visibility uses
inclusive starts, exclusive ends, never exposes drafts, and rejects dates before complete
endpoint coverage. A syntactically valid future `asOf` is not an invalid date: it applies the
same non-draft interval, lifecycle, dependency, and coverage rules and returns an ordinary
visible item, not-found item, or empty collection as applicable. Synthetic temporal fixture
tests exercise every before/at/after boundary; the source-backed smoke request alone is not
sufficient.

### ETag And HEAD

```bash
curl -fsS -D /tmp/grs-get.headers -o /tmp/grs-get.json \
  "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=$COUNTRY_LANGUAGE"
ETAG="$(awk 'BEGIN { IGNORECASE=1 } /^etag:/ { sub(/^[^:]+:[[:space:]]*/, ""); sub(/\r$/, ""); print; exit }' \
  /tmp/grs-get.headers)"
test -n "$ETAG"

STATUS="$(curl -sS -o /tmp/grs-304.body -w '%{http_code}' \
  "${AUTH_READ[@]}" -H "If-None-Match: $ETAG" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=$COUNTRY_LANGUAGE")"
test "$STATUS" = "304"
test ! -s /tmp/grs-304.body

GET_STATUS="$(curl -sS -o /tmp/grs-get.body -w '%{http_code}' \
  "${AUTH_READ[@]}" "$BASE_URL/v1/countries/$COUNTRY_ALPHA2")"
HEAD_STATUS="$(curl -sS -I -o /tmp/grs-head.headers -w '%{http_code}' \
  "${AUTH_READ[@]}" "$BASE_URL/v1/countries/$COUNTRY_ALPHA2")"
test "$GET_STATUS" = "$HEAD_STATUS"
```

Expected: successful catalog responses use `Cache-Control: private, no-cache`, expose the
catalog revision, and return a weak representation-specific ETag. A matching `If-None-Match`
returns bodyless `304`. HEAD applies the same identity, validation, dependency, and resource
pipeline as GET and returns equivalent status and headers without a body. Automated tests also
prove wildcard/list comparison, changed dimensions/revisions, and UTC-date rollover.

### Errors And Access Precedence

Use this shell assertion only for the validation session:

```bash
assert_problem() {
  expected_status="$1"
  expected_code="$2"
  shift 2
  actual_status="$(curl -sS -D /tmp/grs-problem.headers -o /tmp/grs-problem.json \
    -w '%{http_code}' "$@")"
  test "$actual_status" = "$expected_status"
  grep -Eiq '^content-type: application/problem\+json' /tmp/grs-problem.headers
  jq -e --arg code "$expected_code" '
    .code == $code and .status > 0 and
    (.type | startswith("urn:problem-type:geographic-reference:")) and
    (.traceId | type == "string" and length > 0)' /tmp/grs-problem.json
}
```

Exercise representative authentication, validation, absence, coverage, and limit failures:

```bash
assert_problem 401 AUTHENTICATION_REQUIRED "$BASE_URL/v1/countries/$COUNTRY_ALPHA2"
assert_problem 403 ACCESS_DENIED "${AUTH_OBSERVE[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2"
assert_problem 400 INVALID_COUNTRY_CODE_FORMAT "${AUTH_READ[@]}" "$BASE_URL/v1/countries/E1"
assert_problem 404 COUNTRY_NOT_FOUND "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$ABSENT_COUNTRY_CODE"
assert_problem 400 INVALID_DIVISION_CODE_FORMAT "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/1"
assert_problem 404 DIVISION_NOT_FOUND "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$ABSENT_DIVISION_CODE"
assert_problem 400 UNSUPPORTED_IDENTIFIER_SCHEME "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-identifiers/OTHER/$DPA_IDENTIFIER"
assert_problem 404 IDENTIFIER_NOT_FOUND "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-identifiers/$DPA_SCHEME/$ABSENT_DPA_IDENTIFIER"
assert_problem 400 INVALID_IDENTIFIER_FORMAT "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/division-identifiers/$DPA_SCHEME/not-digits"
assert_problem 400 INVALID_LANGUAGE_TAG "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?language=en_US"
assert_problem 400 INVALID_AS_OF_DATE "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2?asOf=2026-02-30"
assert_problem 400 AS_OF_OUTSIDE_CATALOG_COVERAGE "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$DIVISION_COUNTRY_ALPHA2/divisions/$LEAF_DIVISION_CODE?asOf=$BEFORE_COVERAGE"
assert_problem 400 INVALID_PAGINATION "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries?page=0"
assert_problem 400 PAGE_SIZE_LIMIT_EXCEEDED "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries?pageSize=101"
assert_problem 400 INVALID_NAME_TYPE "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$COUNTRY_ALPHA2/names?nameType=POPULAR"
assert_problem 400 DUPLICATE_QUERY_PARAMETER "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries?page=1&page=2"
assert_problem 400 UNSUPPORTED_QUERY_PARAMETER "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries?sort=name"
assert_problem 404 DIVISION_COVERAGE_NOT_AVAILABLE "${AUTH_READ[@]}" \
  "$BASE_URL/v1/countries/$NO_DIVISION_COUNTRY_ALPHA2/divisions/not-a-code?language=en_US&page=0"
assert_problem 401 AUTHENTICATION_REQUIRED "$BASE_URL/q/health/ready"
assert_problem 403 ACCESS_DENIED "${AUTH_READ[@]}" "$BASE_URL/q/health/ready"
```

Expected: authentication precedes input and existence checks; catalog and operational
permissions remain separate; country validation/existence and division coverage precede all
division-specific validation. Problems contain only safe RFC 9457 details and never stack
traces, SQL, credentials, tokens, connection strings, or schema internals.

### Write Methods

Known application paths must reject every unsupported method without parsing catalog input or
requiring identity:

```bash
for method in POST PUT PATCH DELETE OPTIONS; do
  status="$(curl -sS -X "$method" -D /tmp/grs-method.headers \
    -o /tmp/grs-method.json -w '%{http_code}' \
    "$BASE_URL/v1/countries/$COUNTRY_ALPHA2")"
  test "$status" = "405"
  grep -Eiq '^allow: GET, HEAD\r?$' /tmp/grs-method.headers
  jq -e '.code == "METHOD_NOT_ALLOWED" and .status == 405' /tmp/grs-method.json
done
```

Expected: only `GET` and `HEAD` are allowed. The application-owned non-blocking Vert.x
`@RouteFilter` returns this result before authentication and REST routing for every exact known
path template and suppresses automatic Jakarta REST `OPTIONS`. Contract, route-inventory,
architecture, and source tests additionally prove no write path, mutation use case, scheduled
writer, message consumer, startup writer, or hidden management route exists.

## Runtime Privilege Denial

Run the authoritative automated suite first:

```bash
./gradlew --no-daemon runtimePrivilegeTest
```

Then inspect the disposable local role. Relation names and allowed views are defined by
`data-model.md`; the runtime may read only active views in `geographic_api`, and each
database-backed statement compares their revision with the configured expected revision.

```bash
PGPASSWORD="$RUNTIME_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$RUNTIME_DB_USER" -d "$DB_NAME" \
  -c 'SHOW transaction_read_only' \
  -c 'SELECT current_user, count(*) FROM geographic_api.active_countries GROUP BY current_user' \
  -c "SELECT lower('APPROVED_BUILTIN_PROBE') AS approved_builtin_probe" \
  -c "SELECT has_database_privilege(current_user, current_database(), 'CREATE') AS can_create_db_object,
             has_schema_privilege(current_user, 'geographic_api', 'CREATE') AS can_create_api_object,
             has_table_privilege(current_user, 'geographic_api.active_countries', 'SELECT') AS can_read_approved_view,
             (has_table_privilege(current_user, 'geographic_api.active_countries', 'INSERT') OR
              has_table_privilege(current_user, 'geographic_api.active_countries', 'UPDATE') OR
              has_table_privilege(current_user, 'geographic_api.active_countries', 'DELETE') OR
              has_table_privilege(current_user, 'geographic_api.active_countries', 'TRUNCATE')) AS can_write_approved_view"
```

Expected: approved SELECT succeeds, `transaction_read_only` is `on`, approved-view SELECT is
true, required PostgreSQL built-ins used by approved expressions remain callable, and the
database/schema creation and approved-view write checks are false. The default read-only mode is
an overridable diagnostic defense, not the privilege boundary. Prove that a runtime session may
request a read-write transaction while an approved SELECT still succeeds:

```bash
PGPASSWORD="$RUNTIME_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
  -h "$DB_HOST" -p "$DB_PORT" -U "$RUNTIME_DB_USER" -d "$DB_NAME" \
  -c 'BEGIN TRANSACTION READ WRITE' \
  -c 'SHOW transaction_read_only' \
  -c 'SELECT count(*) FROM geographic_api.active_countries' \
  -c 'ROLLBACK'
```

Expected: changing this transaction's mode succeeds and `transaction_read_only` is `off` for
that transaction. Explicit grants must still deny actual mutation and privilege operations.
Internal schema names remain a migration detail and are tested by `runtimePrivilegeTest`, not
duplicated here. Use read-write transactional probes so an incorrectly granted local role
cannot leave a change behind:

```bash
must_deny() {
  sql="$1"
  if PGPASSWORD="$RUNTIME_DB_PASSWORD" psql -X -v ON_ERROR_STOP=1 \
    -h "$DB_HOST" -p "$DB_PORT" -U "$RUNTIME_DB_USER" -d "$DB_NAME" \
    -c "BEGIN TRANSACTION READ WRITE; ${sql}; ROLLBACK;"; then
    printf 'UNEXPECTEDLY ALLOWED: %s\n' "$sql" >&2
    return 1
  fi
}

must_deny 'INSERT INTO geographic_api.active_countries DEFAULT VALUES'
must_deny 'UPDATE geographic_api.active_countries SET default_name = default_name WHERE false'
must_deny 'DELETE FROM geographic_api.active_countries WHERE false'
must_deny 'CREATE TABLE geographic_api.privilege_probe(id integer)'
must_deny 'DROP VIEW geographic_api.active_countries'
must_deny 'ALTER VIEW geographic_api.active_countries OWNER TO geographic_runtime_login'
must_deny 'GRANT SELECT ON geographic_api.active_countries TO geographic_runtime_login'
must_deny 'SET ROLE geographic_owner'
must_deny 'CREATE TEMP TABLE privilege_probe(id integer)'
```

Expected: every actual mutation, DDL, ownership, grant, escalation, and temporary-object probe
is denied even after entering a read-write transaction. The automated suite performs real
internal-table DML and `TRUNCATE` probes and also checks `REFERENCES`, `TRIGGER`, sequence access,
Flyway history, internal and inactive revisions, grant options, role attributes, memberships,
ownership, and default privileges. It denies EXECUTE on application-schema and
mutation-capable routines while proving required PostgreSQL built-ins and every approved SELECT
still work. It must not assert blanket denial of PostgreSQL built-in functions.

## Operations

### Health, Readiness, And Recovery

After obtaining the observe token, wait for startup and readiness:

```bash
until curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/started" >/tmp/grs-started.json; do
  sleep 1
done
curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/live" | jq .
curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/ready" | jq .
```

Expected: startup and readiness are up only for the expected activated revision; each
database-dependent startup/readiness observation uses at most one statement. Liveness describes
process viability and uses zero database statements. Test temporary PostgreSQL failure and
automatic recovery:

```bash
podman stop "$DB_CONTAINER"
curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/live" | jq .
assert_problem 503 GEOGRAPHIC_CATALOG_UNAVAILABLE \
  "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/ready"
assert_problem 503 GEOGRAPHIC_CATALOG_UNAVAILABLE \
  "${AUTH_READ[@]}" "$BASE_URL/v1/catalog"

podman start "$DB_CONTAINER"
until PGPASSWORD="$RUNTIME_DB_PASSWORD" pg_isready \
  -h "$DB_HOST" -p "$DB_PORT" -U "$RUNTIME_DB_USER" -d "$DB_NAME"; do
  sleep 1
done
until curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/health/ready" >/tmp/grs-ready.json; do
  sleep 1
done
```

Expected: dependency loss makes readiness and catalog reads fail atomically with safe `503`
problems but does not make liveness fail. Readiness recovers without restart or catalog write.
Automated tests separately inject query timeout, incomplete activation, migration failure, and
expected-revision mismatch. A local mismatch may also be checked by starting a second runtime
with a deliberately nonexistent expected revision; startup/readiness must return
`CATALOG_REVISION_MISMATCH` and traffic must not be promoted.

### Metrics And Info

```bash
curl -fsS -D /tmp/grs-metrics.headers "${AUTH_OBSERVE[@]}" \
  "$BASE_URL/q/metrics" -o /tmp/grs-metrics.txt
tr -d '\r' < /tmp/grs-metrics.headers \
  | grep -Eiq '^content-type: application/openmetrics-text; version=1\.0\.0; charset=utf-8$'
grep -q '# EOF' /tmp/grs-metrics.txt

curl -fsS "${AUTH_OBSERVE[@]}" "$BASE_URL/q/info" | tee /tmp/grs-info.json | jq .
```

Expected: application-owned metrics use OpenMetrics 1.0 and cover the categories required by
the contract: requests and duration, errors and not-found outcomes, reactive pool utilization
and acquisition duration, query count by route category, readiness, and current catalog
revision. There are no write, import, publication, or command metrics. Info reports application
version, `$BUILD_REVISION`, expected and active `$CATALOG_REVISION`, and declared coverage,
without infrastructure or secret details. Metrics use zero database statements; info uses at
most one statement when database observation is required.

### Graceful Shutdown

Run the automated in-flight-query shutdown test, then stop the local container:

```bash
./gradlew --no-daemon gracefulShutdownTest
time podman stop --time 30 "$RUNTIME_CONTAINER"
podman logs "$RUNTIME_CONTAINER" > /tmp/grs-runtime.log 2>&1
```

Expected: new work is rejected after shutdown begins, accepted bounded work completes or fails
atomically without a partial response, resources close, no catalog row changes, and the process
exits within 30 seconds. Logs are structured JSON with safe revision, trace, route category,
status, and diagnostic context; they contain no token, credential, connection string, or full
catalog response.

## Image And Quadlet Validation

### Non-Root Images

```bash
RUNTIME_USER="$(podman image inspect "$RUNTIME_IMAGE" --format '{{.Config.User}}')"
MIGRATION_USER="$(podman image inspect "$MIGRATION_IMAGE" --format '{{.Config.User}}')"
ROLE_MANAGEMENT_USER="$(podman image inspect "$ROLE_MANAGEMENT_IMAGE" --format '{{.Config.User}}')"
test -n "$RUNTIME_USER" && test "$RUNTIME_USER" != "0" && test "$RUNTIME_USER" != "root"
test -n "$MIGRATION_USER" && test "$MIGRATION_USER" != "0" && test "$MIGRATION_USER" != "root"
test -n "$ROLE_MANAGEMENT_USER" && test "$ROLE_MANAGEMENT_USER" != "0" && test "$ROLE_MANAGEMENT_USER" != "root"
podman run --rm --entrypoint id "$RUNTIME_IMAGE" -u
podman run --rm --entrypoint id "$MIGRATION_IMAGE" -u
podman run --rm --entrypoint id "$ROLE_MANAGEMENT_IMAGE" -u
```

Expected: all configured and effective users are non-root. The JVM runtime exposes only the
required application port, persists no business data, has build revision metadata, and has a
30-second shutdown contract. Native images are outside this feature.

### Quadlet

Validate the source units without installing them:

```bash
QUADLET_UNIT_DIRS="$PWD/deploy/quadlet" \
  /usr/libexec/podman/quadlet -dryrun -user
./gradlew --no-daemon quadletTest
```

Expected: rootless generation succeeds. The migration unit is `Type=oneshot`, uses only the
migration secret, configures the Flyway history schema, and remains successful after completion.
The platform prepare step occurs before that unit. The privileged finalization unit is also
`Type=oneshot`, uses only administrator and new runtime-login secrets, and orders itself `After`
and `Requires` successful migration; its fail-atomic transaction performs runtime-login creation
and migrator hardening. The runtime unit orders itself `After` and `Requires` successful
finalization, receives only the finalized runtime secret, sets a 30-second stop timeout, does not
publish PostgreSQL, and exposes `/v1` only through approved catalog ingress and `/q` only through
approved management ingress. Migration or role-finalization failure prevents runtime startup
and promotion. Finalization and runtime necessarily consume the same runtime credential at
different stages; migration never receives it, finalization alone also receives the administrator
credential, and runtime never receives administrator or migration credentials.

In an isolated deployment-validation account where the units have been installed by the
approved deployment process, validate generated systemd units and ordering:

```bash
systemctl --user daemon-reload
systemd-analyze --user verify geographic-reference-migration.service \
  geographic-reference-finalization.service \
  geographic-reference-runtime.service
systemctl --user start geographic-reference-runtime.service
systemctl --user status geographic-reference-migration.service \
  geographic-reference-finalization.service \
  geographic-reference-runtime.service
```

## Supply-Chain Gates

Use the Phase 0 pinned Trivy OCI image to generate and parse/scan all CycloneDX SBOMs; do not
add a Gradle CycloneDX plugin or defer to another validator. Also scan the repository, all three
images, and Git history.

```bash
export TRIVY_IMAGE="docker.io/aquasec/trivy:0.72.0@sha256:cffe3f5161a47a6823fbd23d985795b3ed72a4c806da4c4df16266c02accdd6f"
export GITLEAKS_IMAGE="docker.io/zricethezav/gitleaks:v8.30.1@sha256:c00b6bd0aeb3071cbcb79009cb16a60dd9e0a7c60e2be9ab65d25e6bc8abbb7f"
[[ "$TRIVY_IMAGE" =~ @sha256:[0-9a-f]{64}$ ]]
[[ "$GITLEAKS_IMAGE" =~ @sha256:[0-9a-f]{64}$ ]]
podman pull "$TRIVY_IMAGE"
podman pull "$GITLEAKS_IMAGE"

mkdir -p build/reports/sbom
podman save --format oci-archive -o /tmp/grs-runtime-image.oci.tar "$RUNTIME_IMAGE"
podman save --format oci-archive -o /tmp/grs-migration-image.oci.tar "$MIGRATION_IMAGE"
podman save --format oci-archive -o /tmp/grs-role-management-image.oci.tar "$ROLE_MANAGEMENT_IMAGE"

trivy_run() {
  podman run --rm --userns=keep-id \
    -v "$PWD:/workspace:Z" -v /tmp:/scan:Z -w /workspace \
    "$TRIVY_IMAGE" "$@"
}

trivy_run fs --format cyclonedx \
  --output /workspace/build/reports/sbom/filesystem.cdx.json /workspace
trivy_run image --input /scan/grs-runtime-image.oci.tar --format cyclonedx \
  --output /workspace/build/reports/sbom/runtime-image.cdx.json
trivy_run image --input /scan/grs-migration-image.oci.tar --format cyclonedx \
  --output /workspace/build/reports/sbom/migration-image.cdx.json
trivy_run image --input /scan/grs-role-management-image.oci.tar --format cyclonedx \
  --output /workspace/build/reports/sbom/role-management-image.cdx.json

for sbom in build/reports/sbom/*.cdx.json; do
  jq -e '.bomFormat == "CycloneDX" and (.specVersion | type == "string") and
         (.components | type == "array")' "$sbom"
  trivy_run sbom --exit-code 1 --severity HIGH,CRITICAL "/workspace/$sbom"
done

podman run --rm --userns=keep-id -v "$PWD:/workspace:Z" -w /workspace \
  "$GITLEAKS_IMAGE" git --redact --no-banner .
trivy_run fs --exit-code 1 --severity HIGH,CRITICAL \
  --scanners vuln,misconfig,secret /workspace
trivy_run image --input /scan/grs-runtime-image.oci.tar \
  --exit-code 1 --severity HIGH,CRITICAL
trivy_run image --input /scan/grs-migration-image.oci.tar \
  --exit-code 1 --severity HIGH,CRITICAL
trivy_run image --input /scan/grs-role-management-image.oci.tar \
  --exit-code 1 --severity HIGH,CRITICAL
```

Expected: the pinned Trivy release generates parseable CycloneDX filesystem and image SBOMs and
successfully consumes each SBOM, and the pinned Gitleaks image scans Git history. The SBOMs are
retained as release evidence; dependency versions are locked and checksum-verified; no
unapproved high or critical vulnerability, leaked secret, embedded credential, source archive,
or unnecessary runtime dependency remains. Scanner exceptions require documented security
approval and expiry; changing a pinned scanner, suppressing a finding, or skipping a scanner is
not acceptance.

## Completion Criteria

Phase 1 implementation is validated only when all of the following are true:

- The legal/source gate passes and the active revision exactly matches the approved derived
  manifest; source-backed smoke values come only from the matching approved generated fixture.
  Before approval, only clearly synthetic fixtures run and no INEC-derived artifact is
  generated, committed, distributed, or promoted.
- All Gradle quality, PostgreSQL 18/Testcontainers, migration, privilege, contract,
  architecture, reactive, hermetic OIDC, static-analysis, documentation, and graceful-shutdown
  tasks pass.
- OpenAPI and runtime inventory contain exactly the 16 approved paths and 32 GET/HEAD
  operations, the packaged OpenAPI is byte-identical, the pre-routing filter owns known-path
  `405` behavior, and no additional production application or management route exists.
- Initial preparation creates only the database, history schema, and temporary-CREATEROLE
  migrator; V001 creates NOLOGIN roles with temporary PostgreSQL creator-admin rows plus the
  required owner SET path, stable registries, views, revocations, and grants; V002 loads stable
  mappings and the candidate; V003 validates and activates. Fail-atomic finalization creates the
  secret runtime login, replaces temporary grants with exact non-admin memberships, revokes
  migrator `CREATEROLE`, transfers required ownership, minimizes recurring migrator privileges,
  and sets runtime-login defaults. Failure injection proves rollback and safe rerun; hardened
  Flyway reconnect/history access and owner `SET ROLE` succeed, while immutable SQL contains no
  credential.
- External Flyway and runtime startup use separate credentials and artifacts; the runtime
  classpath and image contain no Flyway, JDBC driver, migration SQL, or migration secret.
- Catalog metadata, bounded counts, country and division lookups, direct hierarchy,
  localization, temporal coverage, ETag/304, HEAD, stable errors, and access precedence match
  the canonical contract and complete acceptance matrix. Every valid future `asOf` follows
  ordinary temporal semantics.
- Every attempted runtime data, schema, ownership, and privilege change is denied while
  approved active-view SELECTs and required PostgreSQL built-ins succeed, including after the
  overridable transaction read-only default is changed.
- Liveness, startup, readiness, dependency failure/recovery, metrics, info, logs, and graceful
  shutdown match the operational contract without confidential output or partial responses.
  Database-backed observations use at most one statement; liveness, metrics, and pre-database
  outcomes use zero.
- All three OCI images run non-root; Quadlet validates migration-before-finalization-before-runtime
  ordering and secret separation; pinned Trivy CycloneDX generation/consumption and vulnerability
  and misconfiguration scans plus pinned Gitleaks secret scans pass.
- README, DBML, architecture, and every planned database, security, deployment, operations,
  local-development, and testing document are updated with implementation and accurately label
  proposed versus current behavior.

Any failed or skipped item blocks traffic promotion. Preserve task reports, contract and route
inventory, migration output, catalog validation evidence, privilege results, image digests,
SBOMs, scan reports, health/recovery evidence, and approval references with the release record.
