package ch.example.adressen;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * The architecture of the generated code as rules: controller -> repository -> model, nothing
 * the other way round, no cycles. The package names come from .pisa/architecture.plan.json; the file
 * is rewritten by every {@code pisa generate}, so put your own rules into another class.
 * (Rules that match nothing yet pass, so a model without entities is fine.)
 */
@AnalyzeClasses(packagesOf = AdressenSpringApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layersAreRespected = layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Controller").definedBy("ch.example.adressen.controller..")
            .layer("Service").definedBy("ch.example.adressen.service..")
            .layer("Mapper").definedBy("ch.example.adressen.mapper..")
            .layer("Dto").definedBy("ch.example.adressen.dto..")
            .layer("Repository").definedBy("ch.example.adressen.repository..")
            .layer("Model").definedBy("ch.example.adressen.model..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Controller", "Service", "Mapper")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Model").mayOnlyBeAccessedByLayers("Service", "Repository", "Mapper", "Dto");

    @ArchTest
    static final ArchRule packagesHaveNoCycles = slices()
            .matching("ch.example.adressen.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule modelClassesKnowNothingOfWebOrPersistenceAccess = noClasses()
            .that().resideInAPackage("ch.example.adressen.model..")
            .should().dependOnClassesThat().resideInAnyPackage("ch.example.adressen.controller..", "ch.example.adressen.repository..", "ch.example.adressen.service..", "ch.example.adressen.mapper..", "ch.example.adressen.dto..", "org.springframework.web..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule repositoriesAreInterfacesNamedRepository = classes()
            .that().resideInAPackage("ch.example.adressen.repository..")
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Repository")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersAreRestControllersNamedController = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("ch.example.adressen.controller..")
            .andShould().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entitiesAreInTheModelPackage = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("ch.example.adressen.model..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dependenciesAreInjectedThroughConstructors = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .allowEmptyShould(true);
}
