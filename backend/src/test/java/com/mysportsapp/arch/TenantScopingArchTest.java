package com.mysportsapp.arch;

import com.mysportsapp.activity.ActivityRepository;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * A real fitness function, not a no-op: it fails the build the moment
 * someone adds an unscoped {@code ActivityRepository.findById(...)} call
 * anywhere in the tenant-owned {@code activity} or {@code imports}
 * packages, which is exactly the kind of bug that would let one tenant read
 * another tenant's data. The only sanctioned lookup is
 * {@code findByIdAndUserId(id, userId)}.
 */
@AnalyzeClasses(packages = "com.mysportsapp", importOptions = ImportOption.DoNotIncludeTests.class)
public class TenantScopingArchTest {

    @ArchTest
    static final ArchRule activity_and_imports_code_must_not_call_unscoped_findById =
            noClasses()
                    .that().resideInAnyPackage("com.mysportsapp.activity..", "com.mysportsapp.imports..")
                    .should().callMethod(ActivityRepository.class, "findById", Object.class)
                    .because("tenant-owned Activity lookups must go through findByIdAndUserId(id, userId) - "
                            + "which returns empty (mapped to 404, not 403) for another tenant's row - "
                            + "never the unscoped findById(id) inherited from JpaRepository");
}
