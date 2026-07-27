# Geographic Reference Service

Reactive, read-only REST API for ISO countries, localized country names, and
country-specific administrative hierarchies. The service uses Quarkus,
PostgreSQL, Flyway, and a standard response envelope.

## API contract

OpenAPI is the source of truth for paths, parameters, schemas, and response
codes:

- OpenAPI document: <http://localhost:8080/q/openapi>
- Swagger UI in development and test: <http://localhost:8080/q/swagger-ui>

Swagger UI is intentionally not included in the production image. The OpenAPI
document remains available at `/q/openapi` for internal tooling and contract
generation.

### Resource overview

| Resource | Base path | Available queries |
| --- | --- | --- |
| Countries | `/api/v1/countries` | List countries, filter by status, find by UUID or ISO alpha-2, alpha-3, and numeric codes, and list localized names |
| Administrative division types | `/api/v1/countries/{countryId}/division-types` | List country-specific types and find a type by code or UUID |
| Administrative divisions | `/api/v1/countries/{countryId}/administrative-divisions` | List a hierarchy by country, parent, root, or type/status, find divisions, and list identifiers and localized names |

All operations currently use `GET`. Consult OpenAPI instead of duplicating the
complete request and response contract in this file.

## Request tracing headers

Every `/api` request uses these headers:

| Header | Required | Contract |
| --- | --- | --- |
| `user-id` | Yes | Non-blank caller identifier, maximum 128 characters |
| `process-id` | No | Canonical UUID; generated when absent and returned in the response |
| `company-id` | No | Canonical company UUID |

The `/q` management endpoints do not require these headers. Request bodies,
response bodies, authorization headers, and cookies are not written to
application logs.

## Example request

Obtain localized country names for an ISO code type, geographic name type, and
BCP 47 language tag:

```bash
curl --fail --silent --show-error \
  --header 'Accept: application/json' \
  --header 'user-id: api-client' \
  'http://localhost:8080/api/v1/countries/names?codeType=ALPHA2&nameType=COMMON&languageTag=es'
```

Example response:

```json
{
  "status": 200,
  "code": "successful",
  "data": [
    {
      "codeType": "ALPHA2",
      "code": "EC",
      "languageTag": "es",
      "nameType": "COMMON",
      "name": "Ecuador",
      "preferred": true
    }
  ]
}
```

## Requirements

- JDK 25.
- PostgreSQL 18 or a compatible PostgreSQL server.
- Podman or Docker when running tests with Quarkus Dev Services or performing
  a container-based native build.

The application runs Flyway migrations when it starts. The target database
must already exist.

## Database configuration

| Variable | Development default | Production |
| --- | --- | --- |
| `DB_USERNAME` | `postgres` | Required |
| `DB_PASSWORD` | `admin` | Required |
| `DB_REACTIVE_URL` | `postgresql://127.0.0.1:5432/geographic_reference_service` | Required |
| `DB_JDBC_URL` | `jdbc:postgresql://127.0.0.1:5432/geographic_reference_service` | Required |

Do not commit production credentials. The production Quadlet environment-file
format is documented in
[`deploy/quadlet/geographic-reference-service.env.example`](deploy/quadlet/geographic-reference-service.env.example).

## Development

Start the application with live reload:

```bash
./gradlew quarkusDev
```

The Dev UI is available at <http://localhost:8080/q/dev/>.

Run the complete test suite:

```bash
./gradlew test
```

Tests use PostgreSQL through Quarkus Dev Services, so a compatible container
runtime must be available.

## Packaging

Build and run the standard Quarkus application:

```bash
./gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

Build a native executable with a local GraalVM installation:

```bash
./gradlew build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.jar.enabled=false
```

Or build it using the configured native builder container:

```bash
./gradlew build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.package.jar.enabled=false \
  -Dquarkus.native.container-build=true
```

## Documentation

- [VPS deployment with GitHub Actions and Podman Quadlet](docs/deployment/vps-podman-quadlet.md)
- [Architecture diagram](docs/architecture/geographic-reference-service-v1.drawio)
- [Database schema](docs/database/v1-scheme.dbml)
- [Initial reference data](docs/database/initial_data.sql)
