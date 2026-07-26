# geographic-reference-service

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Production deployment

The native-image deployment to a VPS using GitHub Actions, GHCR, rootless
Podman, and Quadlet is documented in
[`docs/deployment/vps-podman-quadlet.md`](docs/deployment/vps-podman-quadlet.md).

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Request tracing headers

Every `/api` request uses the following headers:

- `process-id`: Optional canonical UUID. The service generates one when it is absent and returns the effective value in the response.
- `user-id`: Required non-blank user identifier with a maximum length of 128 characters.
- `company-id`: Optional canonical company UUID.

The `/q` management endpoints do not require these headers. Request bodies, response bodies, authorization headers, and cookies are not written to application logs.

## Localized country-name lookup

Use `GET /api/v1/countries/names` to obtain country names for a selected ISO
3166-1 code type, geographic name type, and BCP 47 language tag.

Required query parameters:

- `codeType`: `ALPHA2`, `ALPHA3`, or `NUMERIC`.
- `nameType`: `OFFICIAL`, `COMMON`, `SHORT`, `ALTERNATIVE`, or `HISTORICAL`.
- `languageTag`: a BCP 47 language tag such as `es`, `en`, or `es-EC`.

For example:

```http
GET /api/v1/countries/names?codeType=ALPHA2&nameType=COMMON&languageTag=es
process-id: 61c55f47-e889-4a34-b61d-07bb060ab496
user-id: api-client
```

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

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/geographic-reference-service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.
