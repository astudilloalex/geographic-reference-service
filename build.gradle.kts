plugins {
    java
    id("io.quarkus")
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project
val archUnitVersion: String by project
val flywayVersion: String by project
val swaggerParserVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-reactive-pg-client")
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-micrometer")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-logging-json")

    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.quarkus:quarkus-test-vertx")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.flywaydb:flyway-core:$flywayVersion")
    testImplementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archUnitVersion")
    testImplementation("io.swagger.parser.v3:swagger-parser-v3:$swaggerParserVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

group = "com.alexastudillo"
version = "1.0.0-SNAPSHOT"

val catalogToolSourceSet =
    sourceSets.create("catalogTool") {
        java.srcDir("src/catalogTool/java")
        resources.srcDir("src/catalogTool/resources")
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += output + compileClasspath
    }

configurations[catalogToolSourceSet.implementationConfigurationName].extendsFrom(
    configurations.implementation.get(),
)
configurations[catalogToolSourceSet.runtimeOnlyConfigurationName].extendsFrom(
    configurations.runtimeOnly.get(),
)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("projectFiles") {
        target(".gitignore", ".dockerignore", "gradle.properties", ".github/**/*.yml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

spotbugs {
    ignoreFailures.set(false)
    showProgress.set(true)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

fun registerVerificationTest(
    taskName: String,
    taskDescription: String,
    vararg testPatterns: String,
) {
    tasks.register<Test>(taskName) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = taskDescription
        dependsOn(tasks.testClasses)
        testClassesDirs =
            sourceSets.test
                .get()
                .output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        filter {
            testPatterns.forEach { includeTestsMatching(it) }
            isFailOnNoMatchingTests = false
        }
    }
}

registerVerificationTest(
    "architectureTest",
    "Runs Clean Architecture and permanent read-only boundary tests.",
    "*.architecture.*",
)
registerVerificationTest(
    "reactiveTest",
    "Runs non-blocking, subscription, and query snapshot consistency tests.",
    "*.support.ReactiveBehaviorTest",
    "*.adapter.out.postgresql.QuerySnapshotConsistencyTest",
)
registerVerificationTest(
    "blockingCanary",
    "Runs the expected-to-fail blocked event-loop detector canary.",
    "*.support.BlockedEventLoopCanaryTest",
)
registerVerificationTest(
    "oidcSecurityTest",
    "Runs the hermetic OIDC and JWT trust-boundary tests.",
    "*.infrastructure.security.OidcSecurityTest",
)
registerVerificationTest(
    "openApiContractTest",
    "Runs OpenAPI, HTTP contract, and method-exclusion tests.",
    "*.contract.ReadOnlyOpenApiTest",
    "*.contract.*ContractTest",
)
registerVerificationTest(
    "routeInventoryTest",
    "Runs the exact production route inventory tests.",
    "*.contract.RouteInventoryTest",
)
registerVerificationTest(
    "packagedOpenApiTest",
    "Runs canonical versus packaged OpenAPI byte-identity tests.",
    "*.contract.PackagedOpenApiTest",
)
registerVerificationTest(
    "documentationTest",
    "Runs current-behavior documentation consistency tests.",
    "*.documentation.*",
)
registerVerificationTest(
    "postgresIntegrationTest",
    "Runs PostgreSQL 18 reactive repository, hierarchy, and query-plan tests.",
    "*.adapter.out.postgresql.*IntegrationTest",
    "*.adapter.out.postgresql.*PlanTest",
    "*.adapter.out.postgresql.QuerySnapshotConsistencyTest",
)
registerVerificationTest(
    "migrationTest",
    "Runs PostgreSQL 18 schema, catalog migration, and finalization tests.",
    "*.migration.*",
    "*.catalogtool.CatalogSourcePipelineTest",
)
registerVerificationTest(
    "runtimePrivilegeTest",
    "Runs the runtime database-role positive and denial matrix.",
    "*.adapter.out.postgresql.RuntimeRolePrivilegeTest",
)
registerVerificationTest(
    "gracefulShutdownTest",
    "Runs bounded in-flight query shutdown tests.",
    "*.infrastructure.operations.GracefulShutdownTest",
)
registerVerificationTest(
    "quadletTest",
    "Runs rootless Podman Quadlet manifest tests.",
    "*.deployment.QuadletTest",
)
