# ADR 0002: Transactional Database Role Finalization

**Status**: Accepted
**Date**: 2026-07-25
**Feature**: `specs/001-read-geographic-catalog`

## Context

The initial Flyway migration must create the geographic owner and runtime privilege roles, while
the long-running service must receive only a SELECT-capable runtime identity. PostgreSQL 18
automatically grants a non-superuser `CREATEROLE` creator an unchangeable, bootstrap-superuser-
granted `ADMIN OPTION` membership in every role it creates. Flyway V001 therefore cannot both
create the roles and establish the accepted recurring least-privilege state by itself.

Catalog migrations must remain separate from the runtime lifecycle and credential. Secret-bearing
login creation cannot be embedded in immutable SQL. Runtime startup must be impossible while the
migrator still has temporary role-administration authority or after partial role hardening.

## Decision

Use three database identities and three non-root OCI images:

- A platform administrator identity exists only for controlled initial preparation and
  finalization.
- `geographic_migrator` runs external Flyway and has temporary `CREATEROLE` only for the initial
  grouped release.
- A secret-managed runtime login inherits only the NOLOGIN `geographic_runtime` role.
- The JVM image contains only the runtime application.
- The Flyway image contains Flyway and immutable catalog migration SQL.
- The role-management image is based on the digest-pinned PostgreSQL 18 image and contains only
  `psql` plus reviewed preparation and finalization scripts.

Initial preparation creates the database, Flyway history schema, and migrator. V001 creates
`geographic_owner` and `geographic_runtime` as NOLOGIN roles. PostgreSQL's temporary automatic
creator-admin rows remain through grouped V001-V003. V001 uses that temporary authority only to
add the non-admin owner SET path needed for object creation; it creates no runtime login and no
runtime SET or INHERIT path.

After migration and integrity verification, a dedicated rootless Quadlet finalization one-shot
runs before runtime. It receives the administrator credential and new runtime-login credential,
takes a stable deployment advisory lock, and executes all changes and assertions in one database
transaction. It removes every temporary creator grant, installs exactly the final
migrator-to-owner and runtime-login-to-runtime memberships, revokes migrator `CREATEROLE`,
transfers required ownership, minimizes recurring migration privileges, and applies the concrete
runtime login's session defaults. It commits only after validating the complete role, ownership,
grant, and setting state.

The script accepts only the complete pre-finalization state or the already validated final state.
Failure rolls back the transaction, and a corrected invocation can be rerun safely. Concurrent
invocations serialize on the same advisory-lock key; the second validates the committed final
state rather than repeating a transition. The runtime unit `Requires` and starts `After` successful
finalization, not merely successful migration.

Secret boundaries are intentionally asymmetric. Migration receives only the migration secret.
Finalization transiently receives administrator and runtime-login bootstrap secrets. Runtime
receives only its runtime secret and never receives administrator or migration credentials. The
finalization and runtime stages necessarily use the same runtime credential at different times;
they never share a process, image lifecycle, or elevated credential.

## Alternatives Considered

### Pre-create Both NOLOGIN Roles Outside Flyway

Rejected because the approved specification requires the initial immutable migration to create
roles and grants, and it would split schema ownership from migration review.

### Leave Migrator CREATEROLE or ADMIN OPTION

Rejected because recurring catalog migration needs only exact Flyway history privileges and
`SET ROLE geographic_owner`. Continuing role-administration authority is excessive.

### Run Flyway as the Platform Administrator

Rejected because catalog SQL would receive broad cluster authority and collapse the migration and
role-administration trust boundaries.

### Finalize in Runtime ExecStartPre

Rejected because the runtime artifact or lifecycle would receive privileged credentials and could
perform role or ownership mutations.

### Manual Role Commands

Rejected because they are not deterministic, fail-atomic, safely rerunnable, or enforceable as a
runtime startup dependency.

## Consequences

- Deployment has migration, finalization, and runtime units plus a controlled initial preparation
  stage.
- The role-management image and administrator secret increase deployment surface but remain
  short-lived and absent from runtime.
- Temporary creator-admin authority exists only between V001 role creation and successful
  finalization; no runtime can start during that interval.
- Future Flyway runs use the hardened migrator without `CREATEROLE` and bracket owner work with
  `SET ROLE geographic_owner` and `RESET ROLE`.
- Password rotation remains a secret-management/finalization concern rather than immutable SQL.

## Risks and Controls

| Risk | Control |
|---|---|
| Partial role hardening | One transaction, final assertions, rollback on every failure |
| Concurrent finalizers | One stable advisory-lock key and a concurrent serialization test |
| Runtime starts in temporary state | Runtime systemd unit requires successful finalization unit |
| Administrator credential reaches runtime | Separate image, unit, secret, process, and lifecycle; runtime configuration scan |
| Runtime credential disclosure during creation | Secret mount/input only; no SQL literal, image layer, log, or release artifact |
| Unexpected membership or owner | Finalization aborts instead of attempting heuristic repair |
| Recurring migrator privilege escalation | Exact catalog tests plus negative GRANT/role probes |
| Role-management image drift | Digest-pinned base, non-root user, SBOM, vulnerability and secret scans |

## Validation Criteria

- PostgreSQL 18 tests prove the temporary bootstrap-granted creator-admin rows and owner SET path
  after V001-V003, and prove runtime cannot start in that state.
- Failure is injected after every finalization stage; each attempt leaves the complete
  pre-finalization state after rollback.
- A corrected rerun succeeds, and another run against the accepted final state is harmless.
- Two simultaneous finalizers serialize on the same advisory lock, produce one transition, and
  make the second invocation validate the final state successfully.
- Final `pg_auth_members` contains exactly one non-admin owner SET row for the migrator and one
  non-admin inherited runtime row for the runtime login, with no migrator/runtime row.
- Migrator `CREATEROLE` is false; exact ownership and Flyway history privileges remain usable;
  runtime positive and negative privilege probes pass.
- Runtime login defaults and timeout behavior match the data model.
- Quadlet tests prove migration then finalization then runtime ordering and failure propagation.
- Image, configuration, log, and secret scans prove each unit receives only its documented
  credentials and artifacts.

## Reversal Strategy

Before finalization commits, transaction rollback restores the complete pre-finalization state and
runtime remains stopped. After commit, an application rollback stops ingress and runtime, rotates
the runtime credential if needed, and redeploys the previous compatible JVM artifact without
restoring temporary creator-admin authority. A change to this role model requires a reviewed
forward role-management change and a superseding ADR. If the initial database itself must be
abandoned, operators restore the approved predeployment recovery point rather than manually
editing role catalogs.
