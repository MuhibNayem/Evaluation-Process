package com.evaluationservice.domain.enums;

/**
 * Role of an evaluator in a 360° evaluation campaign.
 */
public enum EvaluatorRole {
    /** Self-evaluation */
    SELF,
    /** Peer/colleague evaluation */
    PEER,
    /** Direct supervisor/manager evaluation */
    SUPERVISOR,
    /** Subordinate/direct report evaluation */
    SUBORDINATE,
    /** External evaluator (e.g., client, parent, student) */
    EXTERNAL
}
