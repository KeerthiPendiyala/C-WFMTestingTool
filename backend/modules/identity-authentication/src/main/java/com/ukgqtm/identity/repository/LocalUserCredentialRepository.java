package com.ukgqtm.identity.repository;

import com.ukgqtm.identity.domain.LocalUserCredential;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalUserCredentialRepository extends JpaRepository<LocalUserCredential, UUID> {}
