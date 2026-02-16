package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Percentile rank scoring strategy.
 * Computes the mean percentile rank of answer scores within a question set.
 * <p>
 * Score = average percentile position of each answer relative to
 * the sorted distribution of all answer scores.
 */
public class PercentileRankScoring implements ScoringStrategy {

    @Override
    public Score compute(List<Answer> answers, List<Question> questions) {
        List<BigDecimal> rawScores = new ArrayList<>();

        for (Question question : questions) {
            Answer answer = answers.stream()
                    .filter(a -> a.questionId().equals(question.getId()))
                    .findFirst()
                    .orElse(null);

            if (answer != null && answer.value() instanceof Number num) {
                rawScores.add(BigDecimal.valueOf(num.doubleValue()));
            }
        }

        if (rawScores.isEmpty()) {
            return Score.ZERO;
        }

        Collections.sort(rawScores);
        int n = rawScores.size();

        BigDecimal percentileSum = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            // Percentile rank: (rank / n) * 100
            BigDecimal percentile = BigDecimal.valueOf((i + 1.0) / n * 100.0);
            percentileSum = percentileSum.add(percentile);
        }

        BigDecimal meanPercentile = percentileSum.divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
        return Score.of(meanPercentile);
    }
}
