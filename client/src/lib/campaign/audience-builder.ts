export type AssignmentRuleType =
    | "ALL_TO_ALL"
    | "ROUND_ROBIN"
    | "MANAGER_HIERARCHY"
    | "ATTRIBUTE_MATCH";

export interface AudienceParticipant {
    userId: string;
    supervisorId?: string;
    department?: string;
}

export const ASSIGNMENT_RULES: AssignmentRuleType[] = [
    "ALL_TO_ALL",
    "ROUND_ROBIN",
    "MANAGER_HIERARCHY",
    "ATTRIBUTE_MATCH",
];

export const AUDIENCE_SOURCE_TYPES = ["INLINE", "DIRECTORY_SNAPSHOT"] as const;

function asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === "object" && !Array.isArray(value)
        ? (value as Record<string, unknown>)
        : {};
}

function asString(value: unknown, fallback = ""): string {
    if (value === null || value === undefined) {
        return fallback;
    }
    const text = String(value).trim();
    return text || fallback;
}

function asBoolean(value: unknown, fallback: boolean): boolean {
    if (typeof value === "boolean") {
        return value;
    }
    if (typeof value === "string") {
        if (value.toLowerCase() === "true") return true;
        if (value.toLowerCase() === "false") return false;
    }
    return fallback;
}

function asNumber(value: unknown, fallback: number): number {
    if (typeof value === "number" && Number.isFinite(value)) {
        return value;
    }
    if (typeof value === "string") {
        const parsed = Number(value);
        if (Number.isFinite(parsed)) {
            return parsed;
        }
    }
    return fallback;
}

export function participantsFromSourceConfig(config: unknown): AudienceParticipant[] {
    const source = asRecord(config);
    const raw = source.participants;
    if (!Array.isArray(raw)) {
        return [];
    }

    return raw
        .map((item) => {
            const row = asRecord(item);
            const attributes = asRecord(row.attributes);
            const userId = asString(row.userId ?? row.id);
            if (!userId) {
                return null;
            }

            const participant: AudienceParticipant = {
                userId,
                supervisorId: asString(row.supervisorId ?? row.managerId) || undefined,
                department: asString(row.department ?? attributes.department) || undefined,
            };
            return participant;
        })
        .filter((item): item is AudienceParticipant => item !== null);
}

export function sourceConfigFromParticipants(participants: AudienceParticipant[]): Record<string, unknown> {
    return {
        participants: participants
            .map((participant) => ({
                userId: participant.userId.trim(),
                supervisorId: participant.supervisorId?.trim() || undefined,
                department: participant.department?.trim() || undefined,
            }))
            .filter((participant) => participant.userId)
            .map((participant) => {
                const normalized: Record<string, unknown> = {
                    userId: participant.userId,
                };
                if (participant.supervisorId) {
                    normalized.supervisorId = participant.supervisorId;
                }
                if (participant.department) {
                    normalized.department = participant.department;
                }
                return normalized;
            }),
    };
}

export function defaultRuleConfig(ruleType: string): Record<string, unknown> {
    switch (ruleType) {
        case "ALL_TO_ALL":
            return {
                evaluatorRole: "PEER",
                allowSelfEvaluation: false,
                maxEvaluatorsPerEvaluatee: 10,
            };
        case "ROUND_ROBIN":
            return {
                evaluatorRole: "PEER",
                allowSelfEvaluation: false,
                evaluatorsPerEvaluatee: 2,
            };
        case "MANAGER_HIERARCHY":
            return {
                evaluatorRole: "SUPERVISOR",
                includeSelfEvaluation: false,
                requireKnownManager: true,
            };
        case "ATTRIBUTE_MATCH":
        default:
            return {
                matchAttribute: "department",
                evaluatorRole: "PEER",
                allowSelfEvaluation: false,
                maxEvaluatorsPerEvaluatee: 3,
            };
    }
}

export function normalizeRuleConfig(ruleType: string, config: unknown): Record<string, unknown> {
    const source = asRecord(config);
    const defaults = defaultRuleConfig(ruleType);

    switch (ruleType) {
        case "ALL_TO_ALL":
            return {
                evaluatorRole: asString(source.evaluatorRole, String(defaults.evaluatorRole)),
                allowSelfEvaluation: asBoolean(
                    source.allowSelfEvaluation,
                    Boolean(defaults.allowSelfEvaluation),
                ),
                maxEvaluatorsPerEvaluatee: Math.max(
                    1,
                    asNumber(source.maxEvaluatorsPerEvaluatee, Number(defaults.maxEvaluatorsPerEvaluatee)),
                ),
            };
        case "ROUND_ROBIN":
            return {
                evaluatorRole: asString(source.evaluatorRole, String(defaults.evaluatorRole)),
                allowSelfEvaluation: asBoolean(
                    source.allowSelfEvaluation,
                    Boolean(defaults.allowSelfEvaluation),
                ),
                evaluatorsPerEvaluatee: Math.max(
                    1,
                    asNumber(source.evaluatorsPerEvaluatee, Number(defaults.evaluatorsPerEvaluatee)),
                ),
            };
        case "MANAGER_HIERARCHY":
            return {
                evaluatorRole: asString(source.evaluatorRole, String(defaults.evaluatorRole)),
                includeSelfEvaluation: asBoolean(
                    source.includeSelfEvaluation,
                    Boolean(defaults.includeSelfEvaluation),
                ),
                requireKnownManager: asBoolean(
                    source.requireKnownManager,
                    Boolean(defaults.requireKnownManager),
                ),
            };
        case "ATTRIBUTE_MATCH":
        default:
            return {
                matchAttribute: asString(source.matchAttribute, String(defaults.matchAttribute)),
                evaluatorRole: asString(source.evaluatorRole, String(defaults.evaluatorRole)),
                allowSelfEvaluation: asBoolean(
                    source.allowSelfEvaluation,
                    Boolean(defaults.allowSelfEvaluation),
                ),
                maxEvaluatorsPerEvaluatee: Math.max(
                    1,
                    asNumber(source.maxEvaluatorsPerEvaluatee, Number(defaults.maxEvaluatorsPerEvaluatee)),
                ),
            };
    }
}
