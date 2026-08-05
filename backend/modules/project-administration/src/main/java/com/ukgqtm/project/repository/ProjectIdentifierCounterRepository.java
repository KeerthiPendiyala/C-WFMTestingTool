package com.ukgqtm.project.repository;

import com.ukgqtm.project.domain.ProjectIdentifierCounter;
import com.ukgqtm.project.domain.ProjectIdentifierCounterId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProjectIdentifierCounterRepository
        extends JpaRepository<ProjectIdentifierCounter, ProjectIdentifierCounterId> {
    @Query(value = "SELECT allocate_project_identifier(:projectId, :identifierType)", nativeQuery = true)
    @Transactional
    int allocate(@Param("projectId") UUID projectId, @Param("identifierType") String identifierType);
}
