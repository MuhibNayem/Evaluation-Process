package com.evaluationservice.domain.rule;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.value.Score;

import java.util.List;

/**
 * Strategy interface for computing scores from answers.
 * Implementations provide different scoring algorithms.
 */
public interface ScoringStrategy {

    /**
     * Computes a score for a set of answers against their corresponding questions.
     *
     * @param answers   the answers submitted by the evaluator
     * @param questions the questions in the section/template
     * @return the computed score
     */
    Score compute(List<Answer> answers, List<Question> questions);
}
