package com.market;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesDddRules;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = "com.market")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule regras_do_ddd_jmolecules = JMoleculesDddRules.all();

    @ArchTest
    static final ArchRule regras_arquitetura_hexagonal = onionArchitecture()
            // Passamos a varrer tanto a pasta model quanto a pasta event como Modelos de Domínio
            .domainModels("com.market..domain.model..", "com.market..domain.event..")
            .applicationServices("com.market..application..")
            .adapter("web", "com.market..adapter.in.web..")
            .adapter("persistence", "com.market..adapter.out.persistence..", "com.market..adapter.out.event..")
            .allowEmptyShould(true);
}
