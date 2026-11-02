package com.platform.workflow.registry.service;

import com.platform.workflow.registry.domain.TaskExecution;
import com.platform.workflow.registry.domain.TaskState;
import com.platform.workflow.registry.domain.TaskTransition;
import com.platform.workflow.registry.domain.WorkflowTask;
import com.platform.workflow.registry.repository.TaskExecutionRepository;
import com.platform.workflow.registry.repository.TaskTransitionRepository;
import com.platform.workflow.registry.repository.WorkflowTaskRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskQueryService {

    private final WorkflowTaskRepository taskRepository;
    private final TaskTransitionRepository transitionRepository;
    private final TaskExecutionRepository executionRepository;

    public TaskQueryService(
            WorkflowTaskRepository taskRepository,
            TaskTransitionRepository transitionRepository,
            TaskExecutionRepository executionRepository) {
        this.taskRepository = taskRepository;
        this.transitionRepository = transitionRepository;
        this.executionRepository = executionRepository;
    }

    public Page<WorkflowTask> listTasks(TaskState state, Pageable pageable) {
        if (state == null) {
            return taskRepository.findAll(pageable);
        }
        return taskRepository.findByState(state, pageable);
    }

    public WorkflowTask getTask(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public List<TaskTransition> getTransitions(String taskId) {
        getTask(taskId);
        return transitionRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    public List<TaskExecution> getExecutions(String taskId) {
        getTask(taskId);
        return executionRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }
}
