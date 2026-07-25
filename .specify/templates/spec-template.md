# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

## Scope and Boundaries *(mandatory)*

### In Scope

- [Business capabilities and geographic records this feature owns]

### Non-Goals

- [Explicitly excluded behavior, including any bounded-context exclusions affected by
  this request]

### Actors and Access

- [Actor or consuming system, authentication requirement, authorization permission, and
  whether read access is public or internal]

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently - e.g., "Can be fully tested by [specific action] and delivers [specific value]"]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - [Brief Title] (Priority: P3)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- What happens when [boundary condition]?
- How does system handle [error scenario]?
- What happens during concurrent or repeated requests?
- What happens at lifecycle and temporal boundaries?
- What happens after partial external, import, or migration failure?

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: System MUST [specific capability, e.g., "resolve an active country by
  alpha-2 code"]
- **FR-002**: System MUST [validation, e.g., "reject an invalid canonical division code"]
- **FR-003**: Authorized catalog managers MUST be able to [domain action, e.g., "activate
  a validated draft division"]
- **FR-004**: System MUST [data requirement, e.g., "persist source authority and revision"]
- **FR-005**: System MUST [behavior, e.g., "preserve historical identifier resolution"]

*Example of marking unclear requirements:*

- **FR-006**: Read access MUST be [NEEDS CLARIFICATION: public or authenticated internal
  access has not been decided]
- **FR-007**: Retriable commands MUST retain idempotency records for
  [NEEDS CLARIFICATION: retention period not specified]

### Error Behavior *(mandatory)*

- **ER-001**: System MUST [define observable error behavior and stable application error
  code for each failure class]
- **ER-002**: HTTP errors MUST [define RFC 9457 status, problem type, safe detail, and
  retry behavior where applicable]

### Security and Audit *(mandatory)*

- **SR-001**: [Define authentication, authorization scopes or permissions, and access
  visibility]
- **SR-002**: [Define principal-derived audit identity and confidential-data handling]

### Transaction and Consistency Boundaries *(mandatory for state or data changes)*

- **TR-001**: [Define the atomic state change, concurrency behavior, idempotency, and work
  that MUST remain outside the transaction]

### Lifecycle and Temporal Behavior *(include when applicable)*

- **LR-001**: [Define allowed and forbidden lifecycle transitions, visibility, update
  rights, historical queries, and legal temporal combinations]

### Contract Impact *(include for HTTP or event capabilities)*

- **CR-001**: [Identify the canonical OpenAPI or AsyncAPI contract change, compatibility
  strategy, pagination, filtering, sorting, errors, idempotency, and concurrency]

### Data and Migration Impact *(include for persisted data changes)*

- **DR-001**: [Define new or changed data, integrity rules, identifiers, provenance,
  migration and recovery impact, and empty plus upgrade validation]

### Key Entities *(include if feature involves data)*

- **[Country or other aggregate]**: [What it represents and its business attributes]
- **[Administrative division or related entity]**: [What it represents and its
  identity-based relationships]

## Success Criteria *(mandatory)*

<!--
  ACTION REQUIRED: Define measurable, technology-agnostic outcomes supported by the
  described business need or an approved workload. Do not invent arbitrary performance
  targets. If the feature is performance-sensitive, include the approved catalog size,
  request volume, page size, hierarchy depth, access patterns, import size, concurrency,
  response objectives, and resource limits.
-->

### Measurable Outcomes

- **SC-001**: [Measurable outcome, e.g., "A catalog manager can complete the defined
  lifecycle operation using one documented workflow"]
- **SC-002**: [Measurable outcome derived from an approved workload or evidence]
- **SC-003**: [Integrity outcome, e.g., "Every invalid hierarchy transition is rejected
  with the specified error"]
- **SC-004**: [Consumer outcome, e.g., "A retired identifier resolves according to the
  documented historical rules"]

## Assumptions

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right assumptions based on reasonable defaults
  chosen when the feature description did not specify certain details.
-->

- [Assumption about consumers, e.g., "Callers use published logical identifiers"]
- [Assumption about scope, e.g., "Postal codes remain outside this feature"]
- [Assumption about data, e.g., "The source authority supplies one identifiable revision"]
- [Dependency, e.g., "Validated principals are supplied by the approved identity architecture"]

## Dependencies and Risks

- [External dependency, failure mode, and ownership]
- [Known risk, consequence, and required mitigation or decision]

## Documentation Impact

- [README, architecture, C4, API, database, migration, security, deployment, runbook,
  ADR, local-development, or testing documentation that MUST change]
