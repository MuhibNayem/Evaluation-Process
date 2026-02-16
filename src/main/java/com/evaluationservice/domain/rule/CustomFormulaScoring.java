package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom formula scoring strategy.
 * Evaluates admin-defined formulas with question-level score variables.
 * <p>
 * Supported formula syntax:
 * <ul>
 * <li>{@code AVG} — average of all answer scores</li>
 * <li>{@code WEIGHTED} — weighted combination using question weights</li>
 * <li>{@code MIN} — minimum score</li>
 * <li>{@code MAX} — maximum score</li>
 * </ul>
 * Falls back to weighted average if formula is unrecognised.
 */
public class CustomFormulaScoring implements ScoringStrategy {

    private final String formula;

    public CustomFormulaScoring(String formula) {
        if (formula == null || formula.isBlank()) {
            throw new IllegalArgumentException("Custom formula cannot be null or blank");
        }
        this.formula = formula.trim().toUpperCase();
    }

    @Override
    public Score compute(List<Answer> answers, List<Question> questions) {
        if (answers.isEmpty() || questions.isEmpty()) {
            return Score.ZERO;
        }

        Map<String, BigDecimal> questionScores = new HashMap<>();
        Map<String, BigDecimal> questionWeights = new HashMap<>();

        for (Question question : questions) {
            Answer answer = answers.stream()
                    .filter(a -> a.questionId().equals(question.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer != null && answer.value() instanceof Number num) {
                questionScores.put(question.getId(), BigDecimal.valueOf(num.doubleValue()));
                questionWeights.put(question.getId(), question.getWeight().value());
            }
        }

        if (questionScores.isEmpty()) {
            return Score.ZERO;
        }

        BigDecimal result = evaluateFormula(questionScores, questionWeights);
        return Score.of(result);
    }

    private BigDecimal evaluateFormula(Map<String, BigDecimal> scores, Map<String, BigDecimal> weights) {
        return switch (formula) {
            case "AVG" -> computeAverage(scores);
            case "WEIGHTED" -> computeWeighted(scores, weights);
            case "MIN" -> computeMin(scores);
            case "MAX" -> computeMax(scores);
            default -> computeWeighted(scores, weights); // safe fallback
        };
    }

    private BigDecimal computeAverage(Map<String, BigDecimal> scores) {
        BigDecimal sum = scores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeWeighted(Map<String, BigDecimal> scores, Map<String, BigDecimal> weights) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (var entry : scores.entrySet()) {
            BigDecimal weight = weights.getOrDefault(entry.getKey(), BigDecimal.ONE);
            weightedSum = weightedSum.add(entry.getValue().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }
        return totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal computeMin(Map<String, BigDecimal> scores) {
        return scores.values().stream().reduce(BigDecimal::min).orElse(BigDecimal.ZERO);
    }

    private BigDecimal computeMax(Map<String, BigDecimal> scores) {
        return scores.values().stream().reduce(BigDecimal::max).orElse(BigDecimal.ZERO);
    }
}
