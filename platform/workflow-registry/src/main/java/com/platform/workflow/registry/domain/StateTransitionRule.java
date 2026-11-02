package com.platform.workflow.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "state_transition_rule")
public class StateTransitionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "from_state", nullable = false, length = 32)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 32)
    private String toState;

    public String getFromState() {
        return fromState;
    }

    public String getToState() {
        return toState;
    }
}
