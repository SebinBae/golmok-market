package com.golmok.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.golmok")
class ArchitectureTest {

    @ArchTest
    static final ArchRule core는_어댑터를_모른다 = noClasses()
            .that().resideInAPackage("com.golmok.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.golmok.api..",
                    "com.golmok.web..");

    @ArchTest
    static final ArchRule core는_HTTP를_모른다 = noClasses()
            .that().resideInAPackage("com.golmok.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..",
                    "jakarta.servlet..");
}
