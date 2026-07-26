# geographic-reference-service

Geographic Reference Service is intended to become the runtime read-only system of record for
global geographic reference data. The approved design exposes only safe query operations
through `GET` and `HEAD`; it does not provide administrative, import, publication,
lifecycle-command, generic CRUD, or mutation endpoints.

> **Current status**: This repository is an initial scaffold. The approved behavior and
> implementation design are defined in
> [`specs/001-read-geographic-catalog`](specs/001-read-geographic-catalog/); catalog routes,
> PostgreSQL migrations, security, observability, and deployment units are not implemented
> yet. The feature contract permits `GET` and `HEAD` only and does not require application
> `OPTIONS`.

Schema and catalog data are maintained exclusively through reviewed, immutable Flyway
SQL migrations. Flyway runs outside the application with a dedicated migration identity.
The long-running application uses a separate PostgreSQL identity limited to approved
read privileges and MUST NOT receive migration credentials or configure runtime JDBC.

All repository work is governed by the
[project constitution](.specify/memory/constitution.md), including its geographic
bounded context, Java 25 reactive stack, Clean Architecture for queries, contract-first
read-only API, PostgreSQL 18 integrity, SQL catalog governance, test-first verification,
and JVM/Podman Quadlet deployment rules.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

Before starting the application, apply required Flyway migrations through the controlled
external migration process with the migration identity. Then run dev mode with the
read-only runtime database identity:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

Application startup MUST NOT execute Flyway with the runtime identity. Local and CI
configuration MUST preserve the same separation of migration and runtime credentials
used in deployment.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Native executable policy

The approved production baseline is the Java 25 JVM. Native compilation is not part of
the initial runtime and MUST NOT be adopted unless a constitutional amendment and
approved ADR document the required benchmarks, compatibility validation, native
integration tests, and operational acceptance.

After that approval, a native executable can be evaluated using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/geographic-reference-service-1.0.0-SNAPSHOT-runner`

This command is evaluation guidance only; it does not authorize a native production
deployment.
