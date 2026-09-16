package com.mysportsapp.arch;

import com.mysportsapp.activity.ActivityRepository;
import com.mysportsapp.integrations.strava.StravaConnectionRepository;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * A real fitness function, not a no-op: it fails the build the moment
 * someone adds an unscoped {@code findById(...)} call on a tenant-owned
 * repository, which is exactly the kind of bug that would let one tenant
 * read another tenant's data.
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

    @ArchTest
    static final ArchRule strava_integration_code_must_not_call_unscoped_findById =
            noClasses()
                    .that().resideInAPackage("com.mysportsapp.integrations.strava..")
                    .should().callMethod(StravaConnectionRepository.class, "findById", Object.class)
                    .because("a StravaConnection must be looked up by findByUserId(userId) (the acting user) "
                            + "or findByStravaAthleteId(id) (only for resolving an inbound webhook event to a "
                            + "user, in a future phase) - never the unscoped findById(id) inherited from "
                            + "JpaRepository, which has no tenant check at all");
}
