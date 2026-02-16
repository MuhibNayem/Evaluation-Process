package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;
import com.evaluationservice.domain.value.Weight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes a weighted average score from answers.
 * Each question's weight is used to proportionally contribute to the total
 * score.
 */
public class WeightedAverageScoring implements ScoringStrategy {

    @Override
    public Score compute(List<Answer> answers, List<Question> questions) {
        if (answers.isEmpty() || questions.isEmpty()) {
            return Score.ZERO;
        }

        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Answer answer : answers) {
            Question question = questionMap.get(answer.questionId());
            if (question == null)
                continue;

            double numericValue = answer.numericValue();
            Weight weight = question.getWeight();

            weightedSum = weightedSum.add(
                    BigDecimal.valueOf(numericValue).multiply(weight.value()));
            totalWeight = totalWeight.add(weight.value());
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return Score.ZERO;
        }

        BigDecimal result = weightedSum.divide(totalWeight, Score.SCALE, RoundingMode.HALF_UP);
        return Score.of(result);
    }
}
