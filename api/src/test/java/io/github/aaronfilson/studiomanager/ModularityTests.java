package io.github.aaronfilson.studiomanager;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** The module structure is part of the architecture, so it's tested like the rest of it. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(StudioManagerApplication.class);

    @Test
    void modulesOnlyUseEachOthersPublicApi() {
        modules.verify();
    }

    @Test
    void writesModuleDiagrams() {
        // C4 and UML component diagrams in build/spring-modulith-docs
        new Documenter(modules).writeDocumentation();
    }

}
