package com.evaluationservice.application.service;

import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.entity.Question;
import com.evaluationservice.domain.entity.Section;
import com.evaluationservice.domain.entity.SectionScore;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.rule.ScoringStrategy;
import com.evaluationservice.domain.rule.ScoringStrategyFactory;
import com.evaluationservice.domain.value.Score;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service responsible for computing evaluation scores.
 * Delegates to the appropriate ScoringStrategy based on template configuration.
 */
@Service
public class ScoringService {

    /**
     * Computes section-level and total scores for an evaluation.
     *
     * @param evaluation the evaluation with submitted answers
     * @param template   the template defining sections, questions, and scoring
     * @return the list of section scores
     */
    public List<SectionScore> computeSectionScores(Evaluation evaluation, Template template) {
        ScoringStrategy strategy;
        if (template.getScoringMethod() == com.evaluationservice.domain.enums.ScoringMethod.CUSTOM_FORMULA) {
            strategy = ScoringStrategyFactory.forCustomFormula(template.getCustomFormula());
        } else {
            strategy = ScoringStrategyFactory.forMethod(template.getScoringMethod());
        }

        List<SectionScore> sectionScores = new ArrayList<>();
        for (Section section : template.getSections()) {
            List<Answer> sectionAnswers = evaluation.answersForSection(section);
            List<Question> questions = section.getQuestions();

            Score sectionScore = strategy.compute(sectionAnswers, questions);
            Score maxPossible = Score.of(questions.size() * 10.0); // assuming max per question is 10

            sectionScores.add(new SectionScore(
                    section.getId(),
                    section.getTitle(),
                    sectionScore,
                    maxPossible,
                    sectionAnswers.size(),
                    questions.size()));
        }
        return sectionScores;
    }

    /**
     * Computes the overall weighted total score from section scores and their
     * weights.
     */
    public Score computeTotalScore(List<SectionScore> sectionScores, Template template) {
        if (sectionScores.isEmpty())
            return Score.ZERO;

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        List<Section> sections = template.getSections();
        for (int i = 0; i < sectionScores.size() && i < sections.size(); i++) {
            SectionScore ss = sectionScores.get(i);
            Section section = sections.get(i);

            weightedSum = weightedSum.add(
                    ss.score().value().multiply(section.getWeight().value()));
            totalWeight = totalWeight.add(section.getWeight().value());
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0)
            return Score.ZERO;
        return Score.of(weightedSum).divide(totalWeight);
    }
}
