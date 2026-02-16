package com.evaluationservice.domain.enums;

/**
 * Scoring method used to calculate evaluation results.
 */
public enum ScoringMethod {
    /** Weighted average of all section scores */
    WEIGHTED_AVERAGE,
    /** Simple average (equal weights) */
    SIMPLE_AVERAGE,
    /** Median score across all questions */
    MEDIAN,
    /** Percentile rank relative to all evaluations in the campaign */
    PERCENTILE_RANK,
    /** Custom formula defined by admin */
    CUSTOM_FORMULA
}
