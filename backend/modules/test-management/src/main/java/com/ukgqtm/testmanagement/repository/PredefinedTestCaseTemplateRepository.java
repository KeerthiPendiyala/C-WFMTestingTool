package com.ukgqtm.testmanagement.repository;

import com.ukgqtm.testmanagement.domain.PredefinedTestCaseTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PredefinedTestCaseTemplateRepository extends JpaRepository<PredefinedTestCaseTemplate, UUID> {
    @Query("""
            select template
            from PredefinedTestCaseTemplate template
            where template.deletedAt is null
              and template.active = true
              and template.suiteId = :suiteId
              and (template.tenantId is null or template.tenantId = :tenantId)
            order by template.header asc
            """)
    List<PredefinedTestCaseTemplate> findAvailableBySuite(
            @Param("tenantId") String tenantId, @Param("suiteId") UUID suiteId);

    @Query("""
            select template
            from PredefinedTestCaseTemplate template
            where template.deletedAt is null
              and template.active = true
              and template.id = :id
              and (template.tenantId is null or template.tenantId = :tenantId)
            """)
    Optional<PredefinedTestCaseTemplate> findAvailableById(
            @Param("tenantId") String tenantId, @Param("id") UUID id);

    boolean existsByTenantIdAndTemplateKeyAndDeletedAtIsNull(String tenantId, String templateKey);

    @Query("""
            select count(template) > 0
            from PredefinedTestCaseTemplate template
            where template.deletedAt is null
              and template.tenantId = :tenantId
              and template.templateKey = :templateKey
              and template.id <> :excludedId
            """)
    boolean existsTenantTemplateKeyExcluding(
            @Param("tenantId") String tenantId,
            @Param("templateKey") String templateKey,
            @Param("excludedId") UUID excludedId);
}
