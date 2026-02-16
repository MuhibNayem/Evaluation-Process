package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.CampaignPersistencePort;
import com.evaluationservice.domain.entity.Campaign;
import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.mapper.DomainEntityMapper;
import com.evaluationservice.infrastructure.repository.CampaignRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CampaignAdapter implements CampaignPersistencePort {

    private final CampaignRepository repository;
    private final DomainEntityMapper mapper;

    public CampaignAdapter(CampaignRepository repository, DomainEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Campaign save(Campaign campaign) {
        var entity = mapper.toJpaEntity(campaign);
        var saved = repository.save(entity);
        return mapper.toDomainCampaign(saved);
    }

    @Override
    public Optional<Campaign> findById(CampaignId campaignId) {
        return repository.findById(campaignId.value())
                .map(mapper::toDomainCampaign);
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status, int page, int size) {
        return repository.findByStatus(status.name(), PageRequest.of(page, size))
                .map(mapper::toDomainCampaign)
                .getContent();
    }

    @Override
    public List<Campaign> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .map(mapper::toDomainCampaign)
                .getContent();
    }

    @Override
    public boolean existsById(CampaignId campaignId) {
        return repository.existsById(campaignId.value());
    }
}
