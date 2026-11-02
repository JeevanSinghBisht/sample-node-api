package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.RepoConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoConfigRepository extends JpaRepository<RepoConfig, Long> {

    Optional<RepoConfig> findByFullName(String fullName);
}
