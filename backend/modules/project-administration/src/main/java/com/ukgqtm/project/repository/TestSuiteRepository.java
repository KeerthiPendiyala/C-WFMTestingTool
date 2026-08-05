package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.TestSuite;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {
    @Query("""
            select ts
            from TestSuite ts
            where ts.deletedAt is null
              and ts.active = true
              and (ts.tenantId is null or ts.tenantId = :tenantId)
            order by ts.name asc
            """)
    List<TestSuite> findAvailableSuites(@Param("tenantId") String tenantId);

    @Query("""
            select ts
            from TestSuite ts
            where ts.deletedAt is null
              and (ts.tenantId is null or ts.tenantId = :tenantId)
              and ts.id = :id
            """)
    Optional<TestSuite> findAvailableSuite(@Param("tenantId") String tenantId, @Param("id") UUID id);

    @Query("""
            select ts
            from TestSuite ts
            where ts.deletedAt is null
              and ts.tenantId = :tenantId
              and ts.suiteKey = :suiteKey
            """)
    Optional<TestSuite> findTenantSuiteByKey(@Param("tenantId") String tenantId, @Param("suiteKey") String suiteKey);

    @Query("""
            select count(ts) > 0
            from TestSuite ts
            where ts.deletedAt is null
              and ts.tenantId = :tenantId
              and ts.suiteKey = :suiteKey
              and ts.id <> :excludedId
            """)
    boolean existsTenantSuiteByKeyExcluding(
            @Param("tenantId") String tenantId,
            @Param("suiteKey") String suiteKey,
            @Param("excludedId") UUID excludedId);
}
