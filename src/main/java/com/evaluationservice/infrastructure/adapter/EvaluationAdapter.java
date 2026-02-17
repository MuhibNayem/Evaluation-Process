package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.EvaluationPersistencePort;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.enums.EvaluationStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.infrastructure.mapper.DomainEntityMapper;
import com.evaluationservice.infrastructure.repository.EvaluationJpaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EvaluationAdapter implements EvaluationPersistencePort {

    private final EvaluationJpaRepository repository;
    private final DomainEntityMapper mapper;

    public EvaluationAdapter(EvaluationJpaRepository repository, DomainEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        var entity = mapper.toJpaEntity(evaluation);
        var saved = repository.save(entity);
        return mapper.toDomainEvaluation(saved);
    }

    @Override
    public Optional<Evaluation> findById(EvaluationId evaluationId) {
        return repository.findById(evaluationId.value())
                .map(mapper::toDomainEvaluation);
    }

    @Override
    public Optional<Evaluation> findByAssignmentId(String assignmentId) {
        return repository.findByAssignmentId(assignmentId)
                .map(mapper::toDomainEvaluation);
    }

    @Override
    public List<Evaluation> findByCampaignId(CampaignId campaignId, int page, int size) {
        return repository.findByCampaignId(campaignId.value(), PageRequest.of(page, size))
                .map(mapper::toDomainEvaluation)
                .getContent();
    }

    @Override
    public List<Evaluation> findByEvaluateeId(String evaluateeId, int page, int size) {
        return repository.findByEvaluateeId(evaluateeId, PageRequest.of(page, size))
                .map(mapper::toDomainEvaluation)
                .getContent();
    }

    @Override
    public List<Evaluation> findCompletedByCampaignAndEvaluatee(CampaignId campaignId, String evaluateeId) {
        return repository.findByCampaignIdAndEvaluateeIdAndStatus(
                campaignId.value(), evaluateeId, EvaluationStatus.COMPLETED.name())
                .stream()
                .map(mapper::toDomainEvaluation)
                .toList();
    }

    @Override
    public boolean existsByAssignmentId(String assignmentId) {
        return repository.existsByAssignmentId(assignmentId);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByStatus(String status) {
        return repository.countByStatus(status);
    }

    @Override
    public List<Evaluation> findRecentUpdated(int limit) {
        return repository.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, Math.max(limit, 1))).stream()
                .map(mapper::toDomainEvaluation)
                .toList();
    }
}
