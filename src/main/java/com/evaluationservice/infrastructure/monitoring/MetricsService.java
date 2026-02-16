package com.evaluationservice.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;

/**
 * Service for custom metrics collection and monitoring.
 * Provides methods to track business-specific metrics.
 */
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Increments a counter for evaluation creations.
     */
    public void incrementEvaluationCreated() {
        Counter.builder("evaluations.created")
                .description("Number of evaluations created")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Increments a counter for evaluation submissions.
     */
    public void incrementEvaluationSubmitted() {
        Counter.builder("evaluations.submitted")
                .description("Number of evaluations submitted")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records the time taken to process an evaluation.
     */
    public void recordEvaluationProcessingTime(long durationMs) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("evaluations.processing.time")
                .description("Time taken to process an evaluation")
                .register(meterRegistry));
    }

    /**
     * Increments a counter for evaluation results.
     */
    public void incrementEvaluationResult(String status) {
        Counter.builder("evaluations.results")
                .description("Number of evaluation results by status")
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Tracks the number of active evaluations.
     */
    public void trackActiveEvaluations(int count) {
        AtomicInteger activeCount = new AtomicInteger(count);
        Gauge.builder("evaluations.active", activeCount, AtomicInteger::get)
                .description("Number of active evaluations")
                .register(meterRegistry);
    }

    /**
     * Records the score distribution for an evaluation.
     */
    public void recordScoreDistribution(String evaluationId, double score) {
        AtomicReference<Double> scoreRef = new AtomicReference<>(score);
        Gauge.builder("evaluations.scores", scoreRef, AtomicReference::get)
                .description("Score distribution for evaluations")
                .tag("evaluation_id", evaluationId)
                .register(meterRegistry);
    }
}