package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.TaskTransition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTransitionRepository extends JpaRepository<TaskTransition, Long> {

    Optional<TaskTransition> findByIdempotencyKey(String idempotencyKey);

    List<TaskTransition> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
