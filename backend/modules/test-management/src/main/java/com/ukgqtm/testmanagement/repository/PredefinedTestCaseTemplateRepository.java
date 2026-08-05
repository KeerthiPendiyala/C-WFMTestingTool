package com.ukgqtm.testmanagement.repository;

import com.ukgqtm.testmanagement.domain.PredefinedTestCaseTemplate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PredefinedTestCaseTemplateRepository extends JpaRepository<PredefinedTestCaseTemplate, UUID> {}
