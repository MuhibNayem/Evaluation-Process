package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.enums.ScoringMethod;

/**
 * Factory for obtaining the appropriate ScoringStrategy based on ScoringMethod.
 * <p>
 * For {@link ScoringMethod#CUSTOM_FORMULA}, call
 * {@link #forCustomFormula(String)} with the formula from the template.
 */
public final class ScoringStrategyFactory {

    private ScoringStrategyFactory() {
    }

    private static final WeightedAverageScoring WEIGHTED_AVERAGE = new WeightedAverageScoring();
    private static final SimpleAverageScoring SIMPLE_AVERAGE = new SimpleAverageScoring();
    private static final MedianScoring MEDIAN = new MedianScoring();
    private static final PercentileRankScoring PERCENTILE_RANK = new PercentileRankScoring();

    /**
     * Returns the scoring strategy for the given method.
     * For {@code CUSTOM_FORMULA}, use {@link #forCustomFormula(String)} instead.
     */
    public static ScoringStrategy forMethod(ScoringMethod method) {
        return switch (method) {
            case WEIGHTED_AVERAGE -> WEIGHTED_AVERAGE;
            case SIMPLE_AVERAGE -> SIMPLE_AVERAGE;
            case MEDIAN -> MEDIAN;
            case PERCENTILE_RANK -> PERCENTILE_RANK;
            case CUSTOM_FORMULA ->
                throw new IllegalArgumentException(
                        "CUSTOM_FORMULA requires a formula string — use forCustomFormula(String) instead");
        };
    }

    /**
     * Returns a scoring strategy that evaluates the given custom formula.
     *
     * @param formula the formula keyword (AVG, WEIGHTED, MIN, MAX)
     * @return the custom formula scoring strategy
     */
    public static ScoringStrategy forCustomFormula(String formula) {
        return new CustomFormulaScoring(formula);
    }
}
