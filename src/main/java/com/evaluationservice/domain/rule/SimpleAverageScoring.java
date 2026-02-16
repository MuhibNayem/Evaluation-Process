package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Computes a simple average score (equal weights for all questions).
 */
public class SimpleAverageScoring implements ScoringStrategy {

    @Override
    public Score compute(List<Answer> answers, List<Question> questions) {
        if (answers.isEmpty()) {
            return Score.ZERO;
        }

        double sum = answers.stream()
                .mapToDouble(Answer::numericValue)
                .sum();

        BigDecimal average = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(answers.size()), Score.SCALE, RoundingMode.HALF_UP);

        return Score.of(average);
    }
}
