package com.mysportsapp.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Every lookup here is scoped by {@code userId}. In particular, code outside
 * this repository must never call the inherited {@code findById(UUID)} -
 * use {@link #findByIdAndUserId(UUID, UUID)} instead, which returns empty
 * (mapped to a 404, not a 403) for another tenant's row. This is enforced by
 * {@code com.mysportsapp.arch.TenantScopingArchTest}.
 *
 * <p>Filtered listing goes through {@link JpaSpecificationExecutor} (see
 * {@link ActivitySpecifications}) rather than a hand-written JPQL query with
 * {@code (:param IS NULL OR ...)} clauses - the Postgres JDBC driver cannot
 * always infer the bind type of an optional parameter used only inside such
 * a clause, which surfaces as "could not determine data type of parameter".
 * A Specification only ever adds a predicate for filters that are actually
 * present, so no null filter value is ever bound at all.
 */
public interface ActivityRepository extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

    Optional<Activity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndDedupKey(UUID userId, String dedupKey);
}
