package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.TaskExecution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, String> {

    List<TaskExecution> findByTaskIdOrderByCreatedAtDesc(String taskId);
}
