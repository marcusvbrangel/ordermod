package com.market.order;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTests {

    private static final JavaClasses ORDER_CLASSES = new ClassFileImporter()
            .importPackages("com.market.order");

    @Test
    void domainMustRemainIndependentFromSpringApplicationAndAdapters() {
        noClasses()
                .that().resideInAPackage("..order.internal.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "..order.internal.application..",
                        "..order.internal.adapter.."
                )
                .check(ORDER_CLASSES);
    }

    @Test
    void applicationMustNotDependOnAdapters() {
        noClasses()
                .that().resideInAPackage("..order.internal.application..")
                .should().dependOnClassesThat().resideInAPackage("..order.internal.adapter..")
                .check(ORDER_CLASSES);
    }

    @Test
    void portsMustRemainIndependentFromSpringServicesAndAdapters() {
        noClasses()
                .that().resideInAPackage("..order.internal.application.port..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "..order.internal.application.service..",
                        "..order.internal.adapter.."
                )
                .check(ORDER_CLASSES);
    }

    @Test
    void inboundAdaptersMustNotReachServicesOrOutboundAdaptersDirectly() {
        noClasses()
                .that().resideInAPackage("..order.internal.adapter.in..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..order.internal.application.service..",
                        "..order.internal.application.port.out..",
                        "..order.internal.adapter.out.."
                )
                .check(ORDER_CLASSES);
    }
}
