package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;

import java.util.List;

/**
 * Computes the median score from all answers.
 */
public class MedianScoring implements ScoringStrategy {

    @Override
    public Score compute(List<Answer> answers, List<Question> questions) {
        if (answers.isEmpty()) {
            return Score.ZERO;
        }

        double[] sortedValues = answers.stream()
                .mapToDouble(Answer::numericValue)
                .sorted()
                .toArray();

        double median;
        int size = sortedValues.length;
        if (size % 2 == 0) {
            median = (sortedValues[size / 2 - 1] + sortedValues[size / 2]) / 2.0;
        } else {
            median = sortedValues[size / 2];
        }

        return Score.of(median);
    }
}
