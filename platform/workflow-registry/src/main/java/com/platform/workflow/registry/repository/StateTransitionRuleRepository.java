package com.platform.workflow.registry.repository;

import com.platform.workflow.registry.domain.StateTransitionRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateTransitionRuleRepository extends JpaRepository<StateTransitionRule, Integer> {

    boolean existsByFromStateAndToState(String fromState, String toState);
}
