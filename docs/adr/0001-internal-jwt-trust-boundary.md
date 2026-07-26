# ADR 0001: Internal Gateway and JWT Trust Boundary

**Status**: Accepted
**Date**: 2026-07-25
**Feature**: `specs/001-read-geographic-catalog`

## Context

Geographic Reference Service is an internal, read-only service. The approved gateway is its
only network ingress, but the application must independently authenticate callers and enforce
the exact read or observation permission before parsing catalog input or revealing resource
existence. Gateway-only identity headers would create a spoofing risk if the runtime became
reachable through an unintended path. Opaque-token introspection would add a synchronous
identity-provider dependency to each authorization decision.

The catalog contract requires stable RFC 9457 responses for missing, invalid, and insufficient
credentials. Catalog and operational access are separate capabilities, and neither capability
implies the other.

## Decision

The approved gateway forwards the caller's signed OAuth 2.0 bearer access token unchanged over
the protected internal connection. The application validates JWT access tokens using Quarkus
OIDC and the configured issuer's discovery/JWKS metadata.

The initial token profile is:

- JWS algorithm allowlist: RS256 only.
- Required issuer: the deployment-configured internal issuer.
- Required audience: `geographic-reference-service`.
- Required claims: `iss`, `aud`, `exp`, and non-empty `sub`; validate `nbf` when present.
- Permission claim: space-delimited OAuth `scope`.
- Catalog permission: `geographic-reference.read`.
- Operational permission: `geographic-reference.observe`.
- Token transport: `Authorization: Bearer`; identity headers and query-string tokens are not
  accepted.
- Opaque tokens, introspection fallback, unsigned tokens, algorithm negotiation, and permission
  implication are disabled.

HTTP path security executes before application parameter conversion. Application-owned failure
handling maps missing or invalid authentication to `AUTHENTICATION_REQUIRED` and a valid token
without the exact route permission to `ACCESS_DENIED`, preserving the canonical RFC 9457
contract. Unsupported methods on known paths are rejected by the pre-routing method allowlist
before authentication.

Startup must obtain issuer metadata and at least one usable RS256 verification key within 30
seconds. Until that succeeds, startup and readiness remain down. After successful startup, the
runtime refreshes JWKS every five minutes. A failed refresh retains the last successfully
validated non-empty key set, emits a structured warning and failure metric, and leaves readiness
up because already trusted tokens remain verifiable. An unknown `kid` triggers one rate-limited
refresh attempt per minute; if refresh fails or the key remains unknown, that request returns
the canonical invalid-token `401`. A successful refresh atomically replaces the key set only
after issuer metadata and at least one RS256 key validate. An empty usable key set makes
readiness down. Restarting with the issuer unavailable cannot reuse process memory and therefore
fails startup safely.

Catalog and operational routes remain on the same application listener, but the gateway exposes
them through separate catalog and management ingress policies. Direct runtime ingress is
prohibited. The management network boundary is defense in depth and does not replace the
observation permission.

## Alternatives Considered

### Gateway-Validated Identity Headers

Rejected. It would require a second cryptographic channel or trusted-proxy enforcement to
prevent callers from injecting identity and permission headers. A network routing error could
then become an authentication bypass.

### Gateway-Only Authorization

Rejected. It would violate application authorization and error-precedence requirements and
would not protect direct or misrouted runtime traffic.

### Opaque Token Introspection

Rejected for v1. It adds an identity-provider network call and availability dependency to
authorization without an approved requirement. A future proposal must define caching,
revocation, timeout, and failure semantics.

### mTLS Caller Identity

Not selected for v1. It can authenticate a workload connection but does not by itself provide
the two approved route permissions or the required JWT claim and RFC 9457 behavior. It may be
added later as transport defense only after a separate threat and operations review.

### Multiple JWT Algorithms

Rejected initially. A single approved algorithm reduces downgrade, key-confusion, and test
surface. Moving to ES256 or another algorithm requires identity-platform confirmation and an
ADR update before acceptance.

## Consequences

- Runtime needs OIDC discovery/JWKS access at startup and for five-minute refresh. The last
  successful in-memory key set permits bounded operation through a temporary refresh failure;
  ordinary JWT validation requires no per-request introspection.
- Catalog and observation test clients need separate scopes; a token may contain both only when
  explicitly approved.
- Logs may record stable subject and route metadata but never tokens or complete claims.
- Deployment must block direct network ingress and validate issuer, audience, algorithm, token
  lifetime, and permission behavior through the gateway and directly at the application test
  boundary.
- Changing issuer trust, token type, algorithm set, permission claim, or gateway topology is a
  trust-boundary change and requires this ADR to be amended or superseded.

## Risks and Controls

| Risk | Control |
|---|---|
| Forged or substituted token | Validate signature, RS256 allowlist, issuer, audience, and lifetime |
| Stale signing key | OIDC/JWKS refresh and key-rotation integration tests |
| Scope confusion | Exact scope comparison; read and observe do not imply each other |
| Header spoofing | Ignore forwarded identity/permission headers; bearer token is authoritative |
| Direct runtime exposure | Network policy and deployment validation prohibit unapproved ingress |
| Credential disclosure | Redacted structured logs, no token persistence, secret and log scans |
| Framework-default error leakage | Application-owned 401/403 RFC 9457 response handling and contract tests |
| Issuer/JWKS unavailable at startup | 30-second startup acquisition bound; startup/readiness remain down |
| Temporary JWKS refresh failure | Retain last validated keys, rate-limit unknown-key refresh, emit metric/log |
| Empty or invalid refreshed key set | Reject replacement and make readiness down if no usable set remains |

## Validation Criteria

- Missing, malformed, expired, not-yet-valid, wrong-signature, wrong-issuer, wrong-audience,
  non-RS256, and subjectless tokens return the canonical `401` problem.
- Valid tokens lacking the exact route permission return the canonical `403` problem.
- Read-only tokens cannot access operational routes; observation-only tokens cannot access
  catalog routes; explicitly dual-scoped tokens can access both.
- Authentication and permission failures precede input validation and resource lookup.
- Unsupported methods on known paths return `405` before authentication.
- Hermetic automated tests use an in-process test OIDC/JWKS server and generated test keys;
  they do not depend on a live identity platform.
- Tests cover initial issuer/JWKS timeout, successful startup, five-minute refresh, atomic key
  rotation, refresh failure with cached-key validation, unknown-key rate limiting, empty-key
  readiness failure, and restart while the issuer is unavailable.
- Gateway integration tests repeat the token and precedence matrix through the approved ingress.
- Route, log, image, and secret scans find no accepted identity header, embedded token, private
  key, client secret, or confidential claim output.

## Reversal Strategy

Disable service ingress and roll back to the prior application artifact and gateway policy.
Because this decision changes no catalog data, reversal requires no database migration. A
replacement authentication model must be approved in a superseding ADR and must pass the same
contract, threat, precedence, and operational validation before traffic is restored.
