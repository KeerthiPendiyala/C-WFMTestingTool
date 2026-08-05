package com.ukgqtm.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.ukgqtm", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureBoundaryTest {
    @ArchTest
    static final ArchRule businessModulesMustNotUseProviderSdksDirectly = noClasses()
            .that().resideInAnyPackage("com.ukgqtm..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.azure..",
                    "com.rabbitmq..",
                    "com.openai..",
                    "software.amazon.awssdk..")
            .because("ADR-003 requires provider adapters between business modules and vendor SDKs");

    @ArchTest
    static final ArchRule modulesMustNotReachIntoOtherModuleRepositories = noClasses()
            .that().resideInAnyPackage(
                    "com.ukgqtm.identity..",
                    "com.ukgqtm.project..",
                    "com.ukgqtm.requirements..",
                    "com.ukgqtm.testmanagement..",
                    "com.ukgqtm.ai..",
                    "com.ukgqtm.reporting..",
                    "com.ukgqtm.audit..",
                    "com.ukgqtm.evidence..",
                    "com.ukgqtm.execution..",
                    "com.ukgqtm.connectors..",
                    "com.ukgqtm.validation..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..identity..repository..",
                    "..project..repository..",
                    "..requirements..repository..",
                    "..testmanagement..repository..",
                    "..ai..repository..",
                    "..reporting..repository..",
                    "..audit..repository..",
                    "..evidence..repository..",
                    "..execution..repository..",
                    "..connectors..repository..",
                    "..validation..repository..")
            .because("ADR-001 prohibits direct cross-module repository access");

    @ArchTest
    static final ArchRule controllersMustNotBypassApplicationServicesForRepositories = noClasses()
            .that().resideInAPackage("com.ukgqtm.app.api..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..")
            .because("controllers must go through application services and the central authorization policy");

    @ArchTest
    static final ArchRule protectedProjectControllersMustUseCentralAuthorizationPolicy = classes()
            .that().haveSimpleName("ProjectController")
            .should().dependOnClassesThat().haveFullyQualifiedName("com.ukgqtm.app.security.AuthorizationPolicyService")
            .because("RBAC-01 through RBAC-04 must not be implemented with scattered role checks");
}
