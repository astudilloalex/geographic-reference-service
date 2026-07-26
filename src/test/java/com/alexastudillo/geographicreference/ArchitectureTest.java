package com.alexastudillo.geographicreference;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.alexastudillo.geographicreference";
    private static final String DOMAIN_PACKAGE = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION_PACKAGE = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE_PACKAGE = BASE_PACKAGE + ".infrastructure..";

    private final JavaClasses projectClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

    @Test
    void domainShouldOnlyDependOnItselfAndTheJdk() {
        noClasses()
                .that().resideInAPackage(DOMAIN_PACKAGE)
                .should().dependOnClassesThat().resideOutsideOfPackages(DOMAIN_PACKAGE, "java..")
                .check(projectClasses);
    }

    @Test
    void applicationShouldOnlyDependOnDomainJdkAndMutiny() {
        noClasses()
                .that().resideInAPackage(APPLICATION_PACKAGE)
                .should().dependOnClassesThat().resideOutsideOfPackages(
                        APPLICATION_PACKAGE,
                        DOMAIN_PACKAGE,
                        "java..",
                        "io.smallrye.mutiny..")
                .check(projectClasses);
    }

    @Test
    void innerLayersShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAnyPackage(DOMAIN_PACKAGE, APPLICATION_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE_PACKAGE)
                .check(projectClasses);
    }

    @Test
    void layersShouldBeFreeOfCycles() {
        slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(projectClasses);
    }
}
