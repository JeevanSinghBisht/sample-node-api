package com.platform.workflow.registry.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.workflow.registry.domain.ActorType;
import com.platform.workflow.registry.domain.RepoConfig;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.repository.StateTransitionRuleRepository;
import com.platform.workflow.registry.repository.TaskTransitionRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskTransitionServiceTest {

    @Mock
    private WorkflowTaskRepository taskRepository;

    @Mock
    private TaskTransitionRepository transitionRepository;

    @Mock
    private StateTransitionRuleRepository ruleRepository;

    @Mock
    private OutboxService outboxService;

    private TaskTransitionService service;

    @BeforeEach
    void setUp() {
        service = new TaskTransitionService(
                taskRepository,
                transitionRepository,
                ruleRepository,
                outboxService,
                new ObjectMapper());
    }

    @Test
    void transition_appliesWhenRuleAllows() {
        WorkflowTask task = task("task-1", TaskState.READY);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(transitionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(ruleRepository.existsByFromStateAndToState("READY", "DEVELOPING")).thenReturn(true);

        WorkflowTask updated = service.transition(
                "task-1",
                TaskState.DEVELOPING,
                "spawn_developer",
                "key-1",
                ActorType.N8N,
                "n8n",
                null);

        assertEquals(TaskState.DEVELOPING, updated.getState());
        verify(outboxService).enqueue(any(), any());
    }

    @Test
    void transition_rejectsDisallowedEdge() {
        WorkflowTask task = task("task-1", TaskState.READY);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(task));
        when(transitionRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(ruleRepository.existsByFromStateAndToState("READY", "IN_QA")).thenReturn(false);

        assertThrows(
                InvalidTransitionException.class,
                () -> service.transition(
                        "task-1",
                        TaskState.IN_QA,
                        "skip",
                        "key-2",
                        ActorType.N8N,
                        "n8n",
                        null));
        verify(outboxService, never()).enqueue(any(), any());
    }

    private WorkflowTask task(String id, TaskState state) {
        RepoConfig repo = new RepoConfig();
        repo.setFullName("org/demo");
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setRepoConfig(repo);
        task.setState(state);
        task.setPaused(false);
        task.setIssueNumber(1);
        return task;
    }
}
