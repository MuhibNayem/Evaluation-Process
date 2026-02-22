package com.evaluationservice.api.exception;

import com.evaluationservice.domain.exception.CampaignNotActiveException;
import com.evaluationservice.domain.exception.DomainException;
import com.evaluationservice.domain.exception.DuplicateSubmissionException;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.exception.InvalidStateTransitionException;
import com.evaluationservice.domain.exception.TemplateAlreadyPublishedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler using RFC 9457 Problem Details.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE_URI = "https://api.evaluationservice.com/errors/";

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail handleNotFound(EntityNotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(TemplateAlreadyPublishedException.class)
    ProblemDetail handleTemplatePublished(TemplateAlreadyPublishedException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(CampaignNotActiveException.class)
    ProblemDetail handleCampaignNotActive(CampaignNotActiveException ex) {
        return buildProblemDetail(HttpStatus.valueOf(422), ex);
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    ProblemDetail handleDuplicateSubmission(DuplicateSubmissionException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(DuplicateAssignmentException.class)
    ProblemDetail handleDuplicateAssignment(DuplicateAssignmentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Duplicate Assignment");
        pd.setType(URI.create(ERROR_BASE_URI + "duplicate-assignment"));
        pd.setProperty("campaignId", ex.getCampaignId());
        pd.setProperty("evaluatorId", ex.getEvaluatorId());
        pd.setProperty("evaluateeId", ex.getEvaluateeId());
        pd.setProperty("evaluatorRole", ex.getEvaluatorRole());
        pd.setProperty("existingAssignmentId", ex.getExistingAssignmentId());
        return pd;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStateTransitionException ex) {
        return buildProblemDetail(HttpStatus.valueOf(422), ex);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(DomainException ex) {
        return buildProblemDetail(HttpStatus.valueOf(422), ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Argument");
        pd.setType(URI.create(ERROR_BASE_URI + "invalid-argument"));
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Invalid State");
        pd.setType(URI.create(ERROR_BASE_URI + "invalid-state"));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        pd.setTitle("Validation Failed");
        pd.setType(URI.create(ERROR_BASE_URI + "validation-failed"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create(ERROR_BASE_URI + "internal-error"));
        return pd;
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, DomainException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(toTitle(ex.getClass().getSimpleName()));
        pd.setType(URI.create(ERROR_BASE_URI + toKebabCase(ex.getClass().getSimpleName())));
        pd.setProperty("errorCode", ex.getErrorCode());
        return pd;
    }

    private String toKebabCase(String className) {
        return className.replaceAll("Exception$", "")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }

    private String toTitle(String className) {
        return className.replaceAll("Exception$", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}
