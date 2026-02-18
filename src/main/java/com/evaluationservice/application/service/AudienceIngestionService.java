package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.AudienceIngestionUseCase;
import com.evaluationservice.application.service.audience.AudienceSourceConnector;
import com.evaluationservice.application.service.audience.AudienceSourceConnector.SourceRecord;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.AudienceIngestionRunEntity;
import com.evaluationservice.infrastructure.entity.AudienceIngestionRejectionEntity;
import com.evaluationservice.infrastructure.entity.AudienceGroupEntity;
import com.evaluationservice.infrastructure.entity.AudienceMembershipEntity;
import com.evaluationservice.infrastructure.entity.AudiencePersonEntity;
import com.evaluationservice.infrastructure.repository.AudienceGroupRepository;
import com.evaluationservice.infrastructure.repository.AudienceMembershipRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRejectionRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRunRepository;
import com.evaluationservice.infrastructure.repository.AudiencePersonRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.evaluationservice.infrastructure.service.AudienceMappingProfileService;
import com.evaluationservice.infrastructure.service.AudienceIngestionSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Phase-3 scaffold service for canonical audience ingestion.
 * Connector execution and mapping logic are implemented in subsequent iterations.
 */
@Service
public class AudienceIngestionService implements AudienceIngestionUseCase {

    private static final Pattern PERSON_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._:@-]{1,128}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final TenantRepository tenantRepository;
    private final AudiencePersonRepository audiencePersonRepository;
    private final AudienceGroupRepository audienceGroupRepository;
    private final AudienceMembershipRepository audienceMembershipRepository;
    private final AudienceIngestionRunRepository ingestionRunRepository;
    private final AudienceIngestionRejectionRepository ingestionRejectionRepository;
    private final AudienceMappingProfileService mappingProfileService;
    private final AudienceIngestionSnapshotService snapshotService;
    private final Map<String, AudienceSourceConnector> sourceConnectors;
    private final Map<String, EvaluationServiceProperties.ValidationProfile> validationProfiles;

    AudienceIngestionService(
            TenantRepository tenantRepository,
            AudiencePersonRepository audiencePersonRepository,
            AudienceGroupRepository audienceGroupRepository,
            AudienceMembershipRepository audienceMembershipRepository,
            AudienceIngestionRunRepository ingestionRunRepository,
            AudienceIngestionRejectionRepository ingestionRejectionRepository,
            AudienceMappingProfileService mappingProfileService,
            AudienceIngestionSnapshotService snapshotService,
            List<AudienceSourceConnector> sourceConnectors) {
        this(
                tenantRepository,
                audiencePersonRepository,
                audienceGroupRepository,
                audienceMembershipRepository,
                ingestionRunRepository,
                ingestionRejectionRepository,
                mappingProfileService,
                snapshotService,
                sourceConnectors,
                new EvaluationServiceProperties());
    }

    @Autowired
    public AudienceIngestionService(
            TenantRepository tenantRepository,
            AudiencePersonRepository audiencePersonRepository,
            AudienceGroupRepository audienceGroupRepository,
            AudienceMembershipRepository audienceMembershipRepository,
            AudienceIngestionRunRepository ingestionRunRepository,
            AudienceIngestionRejectionRepository ingestionRejectionRepository,
            AudienceMappingProfileService mappingProfileService,
            AudienceIngestionSnapshotService snapshotService,
            List<AudienceSourceConnector> sourceConnectors,
            EvaluationServiceProperties properties) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository);
        this.audiencePersonRepository = Objects.requireNonNull(audiencePersonRepository);
        this.audienceGroupRepository = Objects.requireNonNull(audienceGroupRepository);
        this.audienceMembershipRepository = Objects.requireNonNull(audienceMembershipRepository);
        this.ingestionRunRepository = Objects.requireNonNull(ingestionRunRepository);
        this.ingestionRejectionRepository = Objects.requireNonNull(ingestionRejectionRepository);
        this.mappingProfileService = Objects.requireNonNull(mappingProfileService);
        this.snapshotService = Objects.requireNonNull(snapshotService);
        Objects.requireNonNull(sourceConnectors);
        this.sourceConnectors = new HashMap<>();
        for (AudienceSourceConnector connector : sourceConnectors) {
            String key = connector.sourceType().toUpperCase(Locale.ROOT);
            this.sourceConnectors.put(key, connector);
        }
        EvaluationServiceProperties resolvedProperties = Objects.requireNonNull(properties);
        this.validationProfiles = resolvedProperties.getAudience().getValidationProfiles() == null
                ? Map.of()
                : resolvedProperties.getAudience().getValidationProfiles();
    }

    @Override
    @Transactional
    public IngestionResult ingest(IngestionRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        if (request.tenantId() == null || request.tenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.sourceType() == null || request.sourceType().isBlank()) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (tenantRepository.existsById(request.tenantId()) == false) {
            throw new IllegalArgumentException("Unknown tenantId: " + request.tenantId());
        }

        String runId = "ingest-" + UUID.randomUUID();
        AudienceIngestionRunEntity run = new AudienceIngestionRunEntity();
        run.setId(runId);
        run.setTenantId(request.tenantId());
        run.setSourceType(request.sourceType().trim().toUpperCase(Locale.ROOT));
        run.setStatus("RUNNING");
        run.setDryRun(request.dryRun());
        run.setStartedAt(Instant.now());
        run.setProcessedRecords(0);
        run.setRejectedRecords(0);
        ingestionRunRepository.save(run);

        try {
            IngestionResult result = process(request, runId);
            run.setStatus("SUCCEEDED");
            run.setProcessedRecords(result.processedRecords());
            run.setRejectedRecords(result.rejectedRecords());
            run.setEndedAt(Instant.now());
            ingestionRunRepository.save(run);
            return result;
        } catch (RuntimeException ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            run.setEndedAt(Instant.now());
            ingestionRunRepository.save(run);
            throw ex;
        }
    }

    @Override
    @Transactional
    public IngestionResult replay(String runId, Boolean dryRunOverride) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId is required");
        }
        AudienceIngestionSnapshotService.Snapshot snapshot = snapshotService.load(runId);
        if (!tenantRepository.existsById(snapshot.tenantId())) {
            throw new IllegalArgumentException("Unknown tenantId in snapshot: " + snapshot.tenantId());
        }

        boolean dryRun = dryRunOverride != null
                ? dryRunOverride
                : ingestionRunRepository.findById(runId).map(AudienceIngestionRunEntity::isDryRun).orElse(false);

        String replayRunId = "ingest-" + UUID.randomUUID();
        AudienceIngestionRunEntity run = new AudienceIngestionRunEntity();
        run.setId(replayRunId);
        run.setTenantId(snapshot.tenantId());
        run.setSourceType(snapshot.sourceType());
        run.setStatus("RUNNING");
        run.setDryRun(dryRun);
        run.setStartedAt(Instant.now());
        run.setProcessedRecords(0);
        run.setRejectedRecords(0);
        ingestionRunRepository.save(run);

        try {
            snapshotService.save(
                    replayRunId,
                    snapshot.tenantId(),
                    snapshot.sourceType(),
                    snapshot.mappingProfileId(),
                    snapshot.sourceConfig(),
                    snapshot.records());
            IngestionResult result = processResolvedRecords(
                    snapshot.tenantId(),
                    dryRun,
                    replayRunId,
                    parseEntityType(snapshot.sourceConfig().get("entityType")),
                    snapshot.records(),
                    resolveValidationProfile(snapshot.sourceConfig().get("validationProfile")));
            run.setStatus("SUCCEEDED");
            run.setProcessedRecords(result.processedRecords());
            run.setRejectedRecords(result.rejectedRecords());
            run.setEndedAt(Instant.now());
            ingestionRunRepository.save(run);
            return result;
        } catch (RuntimeException ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            run.setEndedAt(Instant.now());
            ingestionRunRepository.save(run);
            throw ex;
        }
    }

    private IngestionResult process(IngestionRequest request, String runId) {
        String sourceType = request.sourceType().trim().toUpperCase(Locale.ROOT);
        AudienceSourceConnector connector = sourceConnectors.get(sourceType);
        if (connector == null) {
            throw new IllegalArgumentException("Unsupported sourceType: " + sourceType);
        }

        Map<String, Object> config = request.sourceConfig() == null ? Map.of() : request.sourceConfig();
        List<SourceRecord> records = connector.loadRecords(config);
        AudienceEntityType entityType = parseEntityType(config.get("entityType"));
        ResolvedValidationProfile validationProfile = resolveValidationProfile(config.get("validationProfile"));
        Map<String, String> mappings = mappingProfileService.resolveActiveMappings(
                request.tenantId(),
                request.mappingProfileId(),
                sourceType);
        if (!mappings.isEmpty()) {
            records = records.stream()
                    .map(record -> applyMappings(record, mappings))
                    .toList();
        }
        snapshotService.save(
                runId,
                request.tenantId(),
                sourceType,
                request.mappingProfileId(),
                config,
                records);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("Source must include at least one data record");
        }

        return processResolvedRecords(request.tenantId(), request.dryRun(), runId, entityType, records, validationProfile);
    }

    private IngestionResult processResolvedRecords(
            String tenantId,
            boolean dryRun,
            String runId,
            AudienceEntityType entityType,
            List<SourceRecord> records,
            ResolvedValidationProfile validationProfile) {
        return switch (entityType) {
            case PERSON -> processPersonRecords(tenantId, dryRun, runId, records, validationProfile);
            case GROUP -> processGroupRecords(tenantId, dryRun, runId, records, validationProfile);
            case MEMBERSHIP -> processMembershipRecords(tenantId, dryRun, runId, records, validationProfile);
        };
    }

    private IngestionResult processPersonRecords(
            String tenantId,
            boolean dryRun,
            String runId,
            List<SourceRecord> records,
            ResolvedValidationProfile validationProfile) {
        int processed = 0;
        int rejected = 0;
        Set<String> seenPersonIds = new HashSet<>();
        for (SourceRecord record : records) {
            String personId = firstPresent(record.fields(), "person_id", "id", "user_id");
            if (personId == null || personId.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing person_id", record.rawData());
                rejected++;
                continue;
            }
            if (!isValidPersonId(personId)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid person_id format; allowed [A-Za-z0-9._:@-], max length 128",
                        record.rawData());
                rejected++;
                continue;
            }
            if (!seenPersonIds.add(personId)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Duplicate person_id in source payload: " + personId,
                        record.rawData());
                rejected++;
                continue;
            }

            String displayName = firstPresent(record.fields(), "display_name", "name");
            String email = firstPresent(record.fields(), "email");
            if (validationProfile.requirePersonDisplayName
                    && (displayName == null || displayName.isBlank())) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing display_name", record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.minPersonDisplayNameLength > 0
                    && displayName != null
                    && !displayName.isBlank()
                    && displayName.trim().length() < validationProfile.minPersonDisplayNameLength) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "display_name shorter than minimum length " + validationProfile.minPersonDisplayNameLength,
                        record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.requirePersonEmail && (email == null || email.isBlank())) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing email", record.rawData());
                rejected++;
                continue;
            }
            if (email != null && !isValidEmail(email)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid email format",
                        record.rawData());
                rejected++;
                continue;
            }
            if (email != null && !email.isBlank() && !validationProfile.allowedEmailDomains.isEmpty()) {
                String domain = extractEmailDomain(email);
                if (domain == null || !validationProfile.allowedEmailDomains.contains(domain.toLowerCase(Locale.ROOT))) {
                    saveRejection(
                            runId,
                            tenantId,
                            record.rowNumber(),
                            "Email domain is not allowed by validation profile",
                            record.rawData());
                    rejected++;
                    continue;
                }
            }

            AudiencePersonEntity existing = audiencePersonRepository.findById(personId).orElse(null);
            if (existing != null && !tenantId.equals(existing.getTenantId())) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "person_id already exists in another tenant",
                        record.rawData());
                rejected++;
                continue;
            }

            Boolean activeValue = parseActive(firstPresent(record.fields(), "active"));
            if (activeValue == null) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid active value; allowed: true,false,1,0,yes,no,y,n",
                        record.rawData());
                rejected++;
                continue;
            }

            if (!dryRun) {
                AudiencePersonEntity person = existing == null ? new AudiencePersonEntity() : existing;
                person.setId(personId);
                person.setTenantId(tenantId);
                person.setExternalRef(firstPresent(record.fields(), "external_ref"));
                person.setDisplayName(displayName);
                person.setEmail(email);
                person.setActive(activeValue);
                Instant now = Instant.now();
                person.setUpdatedAt(now);
                if (person.getCreatedAt() == null) {
                    person.setCreatedAt(now);
                }
                audiencePersonRepository.save(person);
            }
            processed++;
        }

        return new IngestionResult(tenantId, runId, dryRun, processed, rejected);
    }

    private IngestionResult processGroupRecords(
            String tenantId,
            boolean dryRun,
            String runId,
            List<SourceRecord> records,
            ResolvedValidationProfile validationProfile) {
        int processed = 0;
        int rejected = 0;
        Set<String> seenGroupIds = new HashSet<>();

        for (SourceRecord record : records) {
            String groupId = firstPresent(record.fields(), "group_id", "id");
            if (groupId == null || groupId.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing group_id", record.rawData());
                rejected++;
                continue;
            }
            if (!isValidPersonId(groupId)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid group_id format; allowed [A-Za-z0-9._:@-], max length 128",
                        record.rawData());
                rejected++;
                continue;
            }
            if (!seenGroupIds.add(groupId)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Duplicate group_id in source payload: " + groupId,
                        record.rawData());
                rejected++;
                continue;
            }

            String groupType = firstPresent(record.fields(), "group_type");
            String name = firstPresent(record.fields(), "name", "display_name");
            String externalRef = firstPresent(record.fields(), "external_ref");
            if (groupType == null || groupType.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing group_type", record.rawData());
                rejected++;
                continue;
            }
            if (name == null || name.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing name", record.rawData());
                rejected++;
                continue;
            }
            if (!validationProfile.allowedGroupTypes.isEmpty()
                    && !validationProfile.allowedGroupTypes.contains(groupType.trim().toUpperCase(Locale.ROOT))) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "group_type not allowed by validation profile: " + groupType,
                        record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.requireGroupExternalRef && (externalRef == null || externalRef.isBlank())) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing external_ref", record.rawData());
                rejected++;
                continue;
            }

            Boolean activeValue = parseActive(firstPresent(record.fields(), "active"));
            if (activeValue == null) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid active value; allowed: true,false,1,0,yes,no,y,n",
                        record.rawData());
                rejected++;
                continue;
            }

            if (!dryRun) {
                AudienceGroupEntity existing = audienceGroupRepository.findById(groupId).orElse(null);
                if (existing != null && !tenantId.equals(existing.getTenantId())) {
                    saveRejection(
                            runId,
                            tenantId,
                            record.rowNumber(),
                            "group_id already exists in another tenant",
                            record.rawData());
                    rejected++;
                    continue;
                }
                AudienceGroupEntity group = existing == null ? new AudienceGroupEntity() : existing;
                group.setId(groupId);
                group.setTenantId(tenantId);
                group.setGroupType(groupType);
                group.setName(name);
                group.setExternalRef(externalRef);
                group.setActive(activeValue);
                Instant now = Instant.now();
                group.setUpdatedAt(now);
                if (group.getCreatedAt() == null) {
                    group.setCreatedAt(now);
                }
                audienceGroupRepository.save(group);
            }
            processed++;
        }
        return new IngestionResult(tenantId, runId, dryRun, processed, rejected);
    }

    private IngestionResult processMembershipRecords(
            String tenantId,
            boolean dryRun,
            String runId,
            List<SourceRecord> records,
            ResolvedValidationProfile validationProfile) {
        int processed = 0;
        int rejected = 0;
        Set<String> membershipKeys = new HashSet<>();

        for (SourceRecord record : records) {
            String personId = firstPresent(record.fields(), "person_id");
            String groupId = firstPresent(record.fields(), "group_id");
            if (personId == null || personId.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing person_id", record.rawData());
                rejected++;
                continue;
            }
            if (groupId == null || groupId.isBlank()) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing group_id", record.rawData());
                rejected++;
                continue;
            }

            String role = firstPresent(record.fields(), "membership_role");
            if (validationProfile.requireMembershipRole && (role == null || role.isBlank())) {
                saveRejection(runId, tenantId, record.rowNumber(), "Missing membership_role", record.rawData());
                rejected++;
                continue;
            }
            String uniqueKey = personId + "|" + groupId + "|" + (role == null ? "" : role);
            if (!membershipKeys.add(uniqueKey)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Duplicate membership in source payload",
                        record.rawData());
                rejected++;
                continue;
            }

            AudiencePersonEntity person = audiencePersonRepository.findById(personId).orElse(null);
            if (person == null || !tenantId.equals(person.getTenantId())) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Unknown person_id for membership: " + personId,
                        record.rawData());
                rejected++;
                continue;
            }
            AudienceGroupEntity group = audienceGroupRepository.findById(groupId).orElse(null);
            if (group == null || !tenantId.equals(group.getTenantId())) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Unknown group_id for membership: " + groupId,
                        record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.requireActivePersonForMembership && !person.isActive()) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "person_id is inactive for membership: " + personId,
                        record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.requireActiveGroupForMembership && !group.isActive()) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "group_id is inactive for membership: " + groupId,
                        record.rawData());
                rejected++;
                continue;
            }
            if (role != null && !role.isBlank() && !validationProfile.membershipRoleAllowedGroupTypes.isEmpty()) {
                List<String> allowedTypes = validationProfile.membershipRoleAllowedGroupTypes
                        .get(role.trim().toUpperCase(Locale.ROOT));
                if (allowedTypes != null && !allowedTypes.isEmpty()
                        && !allowedTypes.contains(group.getGroupType().trim().toUpperCase(Locale.ROOT))) {
                    saveRejection(
                            runId,
                            tenantId,
                            record.rowNumber(),
                            "membership_role not allowed for group_type by validation profile",
                            record.rawData());
                    rejected++;
                    continue;
                }
            }

            Boolean activeValue = parseActive(firstPresent(record.fields(), "active"));
            if (activeValue == null) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid active value; allowed: true,false,1,0,yes,no,y,n",
                        record.rawData());
                rejected++;
                continue;
            }

            Instant validFrom;
            Instant validTo;
            try {
                validFrom = parseInstant(firstPresent(record.fields(), "valid_from"));
                validTo = parseInstant(firstPresent(record.fields(), "valid_to"));
            } catch (IllegalArgumentException ex) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        ex.getMessage(),
                        record.rawData());
                rejected++;
                continue;
            }
            if (validationProfile.requireMembershipValidityWindow && (validFrom == null || validTo == null)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Missing valid_from/valid_to required by validation profile",
                        record.rawData());
                rejected++;
                continue;
            }
            if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
                saveRejection(
                        runId,
                        tenantId,
                        record.rowNumber(),
                        "Invalid membership validity window: valid_to before valid_from",
                        record.rawData());
                rejected++;
                continue;
            }

            if (!dryRun) {
                AudienceMembershipEntity membership = resolveMembership(tenantId, personId, groupId, role);
                if (membership == null) {
                    membership = new AudienceMembershipEntity();
                    membership.setTenantId(tenantId);
                    membership.setPersonId(personId);
                    membership.setGroupId(groupId);
                    membership.setMembershipRole(role);
                    membership.setCreatedAt(Instant.now());
                }
                membership.setActive(activeValue);
                membership.setValidFrom(validFrom);
                membership.setValidTo(validTo);
                membership.setUpdatedAt(Instant.now());
                audienceMembershipRepository.save(membership);
            }
            processed++;
        }

        return new IngestionResult(tenantId, runId, dryRun, processed, rejected);
    }

    private String firstPresent(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String value = fields.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private SourceRecord applyMappings(SourceRecord record, Map<String, String> mappings) {
        Map<String, String> mapped = new HashMap<>();
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String canonicalField = entry.getKey();
            String sourceField = entry.getValue();
            mapped.put(canonicalField, record.fields().get(sourceField));
        }
        return new SourceRecord(record.rowNumber(), mapped, record.rawData());
    }

    private void saveRejection(String runId, String tenantId, int rowNumber, String reason, String rowData) {
        AudienceIngestionRejectionEntity rejection = new AudienceIngestionRejectionEntity();
        rejection.setRunId(runId);
        rejection.setTenantId(tenantId);
        rejection.setRowNumber(rowNumber);
        rejection.setReason(reason);
        rejection.setRowData(rowData);
        rejection.setCreatedAt(Instant.now());
        ingestionRejectionRepository.save(rejection);
    }

    private Boolean parseActive(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> null;
        };
    }

    private boolean isValidPersonId(String personId) {
        return PERSON_ID_PATTERN.matcher(personId).matches();
    }

    private boolean isValidEmail(String email) {
        if (email.length() > 320) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private AudienceMembershipEntity resolveMembership(String tenantId, String personId, String groupId, String role) {
        if (role == null || role.isBlank()) {
            return audienceMembershipRepository
                    .findByTenantIdAndPersonIdAndGroupIdAndMembershipRoleIsNull(tenantId, personId, groupId)
                    .orElse(null);
        }
        return audienceMembershipRepository
                .findByTenantIdAndPersonIdAndGroupIdAndMembershipRole(tenantId, personId, groupId, role)
                .orElse(null);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid timestamp format; expected ISO-8601 for valid_from/valid_to");
        }
    }

    private ResolvedValidationProfile resolveValidationProfile(Object requestedProfile) {
        String profile = requestedProfile == null ? "DEFAULT" : String.valueOf(requestedProfile).trim();
        if (profile.isEmpty()) {
            profile = "DEFAULT";
        }
        EvaluationServiceProperties.ValidationProfile configured = validationProfiles.get(profile);
        if (configured == null) {
            configured = validationProfiles.get(profile.toLowerCase(Locale.ROOT));
        }
        if (configured == null) {
            configured = validationProfiles.get(profile.toUpperCase(Locale.ROOT));
        }
        if (configured == null) {
            configured = validationProfiles.get("default");
        }
        if (configured == null) {
            configured = validationProfiles.get("DEFAULT");
        }
        if (configured == null) {
            return ResolvedValidationProfile.defaultProfile();
        }
        return ResolvedValidationProfile.from(configured);
    }

    private String extractEmailDomain(String email) {
        int at = email.lastIndexOf('@');
        if (at <= 0 || at >= email.length() - 1) {
            return null;
        }
        return email.substring(at + 1).trim();
    }

    private AudienceEntityType parseEntityType(Object value) {
        if (value == null) {
            return AudienceEntityType.PERSON;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PERSON", "PERSONS" -> AudienceEntityType.PERSON;
            case "GROUP", "GROUPS" -> AudienceEntityType.GROUP;
            case "MEMBERSHIP", "MEMBERSHIPS" -> AudienceEntityType.MEMBERSHIP;
            default -> throw new IllegalArgumentException("Unsupported entityType: " + normalized);
        };
    }

    private enum AudienceEntityType {
        PERSON,
        GROUP,
        MEMBERSHIP
    }

    private record ResolvedValidationProfile(
            boolean requirePersonDisplayName,
            int minPersonDisplayNameLength,
            boolean requirePersonEmail,
            Set<String> allowedEmailDomains,
            Set<String> allowedGroupTypes,
            boolean requireGroupExternalRef,
            boolean requireMembershipRole,
            boolean requireMembershipValidityWindow,
            boolean requireActivePersonForMembership,
            boolean requireActiveGroupForMembership,
            Map<String, List<String>> membershipRoleAllowedGroupTypes) {
        static ResolvedValidationProfile defaultProfile() {
            return new ResolvedValidationProfile(
                    false,
                    0,
                    false,
                    Set.of(),
                    Set.of(),
                    false,
                    false,
                    false,
                    false,
                    false,
                    Map.of());
        }

        static ResolvedValidationProfile from(EvaluationServiceProperties.ValidationProfile configured) {
            Map<String, List<String>> roleRules = new HashMap<>();
            if (configured.getMembershipRoleAllowedGroupTypes() != null) {
                for (Map.Entry<String, List<String>> entry : configured.getMembershipRoleAllowedGroupTypes().entrySet()) {
                    if (entry.getKey() == null) {
                        continue;
                    }
                    List<String> normalized = entry.getValue() == null
                            ? List.of()
                            : entry.getValue().stream()
                                    .filter(Objects::nonNull)
                                    .map(v -> v.trim().toUpperCase(Locale.ROOT))
                                    .filter(v -> !v.isEmpty())
                                    .toList();
                    roleRules.put(entry.getKey().trim().toUpperCase(Locale.ROOT), normalized);
                }
            }
            Set<String> emailDomains = configured.getAllowedEmailDomains() == null
                    ? Set.of()
                    : configured.getAllowedEmailDomains().stream()
                            .filter(Objects::nonNull)
                            .map(v -> v.trim().toLowerCase(Locale.ROOT))
                            .filter(v -> !v.isEmpty())
                            .collect(java.util.stream.Collectors.toSet());
            Set<String> groupTypes = configured.getAllowedGroupTypes() == null
                    ? Set.of()
                    : configured.getAllowedGroupTypes().stream()
                            .filter(Objects::nonNull)
                            .map(v -> v.trim().toUpperCase(Locale.ROOT))
                            .filter(v -> !v.isEmpty())
                            .collect(java.util.stream.Collectors.toSet());
            return new ResolvedValidationProfile(
                    configured.isRequirePersonDisplayName(),
                    Math.max(0, configured.getMinPersonDisplayNameLength()),
                    configured.isRequirePersonEmail(),
                    emailDomains,
                    groupTypes,
                    configured.isRequireGroupExternalRef(),
                    configured.isRequireMembershipRole(),
                    configured.isRequireMembershipValidityWindow(),
                    configured.isRequireActivePersonForMembership(),
                    configured.isRequireActiveGroupForMembership(),
                    roleRules);
        }
    }
}
