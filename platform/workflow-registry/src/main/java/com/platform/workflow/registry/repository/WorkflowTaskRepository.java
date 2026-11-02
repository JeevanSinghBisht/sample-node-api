package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, String> {

    @Query("SELECT t FROM WorkflowTask t JOIN t.repoConfig r WHERE r.fullName = :fullName AND t.issueNumber = :issueNumber")
    Optional<WorkflowTask> findByRepoFullNameAndIssueNumber(
            @Param("fullName") String fullName, @Param("issueNumber") int issueNumber);

    Page<WorkflowTask> findByState(TaskState state, Pageable pageable);

    @Query("""
            SELECT t FROM WorkflowTask t
            WHERE t.state = :state AND t.nextRetryAt IS NOT NULL AND t.nextRetryAt <= :now
            """)
    List<WorkflowTask> findRetryDue(@Param("state") TaskState state, @Param("now") Instant now);
}
