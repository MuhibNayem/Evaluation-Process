package com.evaluationservice.domain.enums;

/**
 * Types of questions supported in evaluation templates.
 */
public enum QuestionType {
    /** Single choice from a list of options */
    SINGLE_CHOICE,
    /** Multiple choices from a list of options */
    MULTIPLE_CHOICE,
    /** Likert scale rating (e.g., 1-5, 1-7) */
    LIKERT_SCALE,
    /** Open-ended text response */
    OPEN_TEXT,
    /** Numeric rating on a scale (e.g., 1-10) */
    NUMERIC_RATING,
    /** Yes/No boolean answer */
    BOOLEAN,
    /** Matrix/grid question (multiple sub-questions, same scale) */
    MATRIX,
    /** Ranking of items in order */
    RANKING,
    /** Net Promoter Score (0-10 scale) */
    NPS,
    /** File upload response */
    FILE_UPLOAD;

    /**
     * Whether this question type requires predefined options/choices.
     */
    public boolean requiresOptions() {
        return switch (this) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, RANKING, MATRIX -> true;
            default -> false;
        };
    }
}
