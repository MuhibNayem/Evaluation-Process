<script lang="ts">
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import { Plus, Trash2, Users } from "@lucide/svelte";
    import {
        ASSIGNMENT_RULES,
        AUDIENCE_SOURCE_TYPES,
        defaultRuleConfig,
        type AudienceParticipant,
    } from "$lib/campaign/audience-builder.js";

    let {
        audienceSourceType = $bindable("INLINE"),
        assignmentRuleType = $bindable("ATTRIBUTE_MATCH"),
        participants = $bindable([] as AudienceParticipant[]),
        ruleConfig = $bindable({} as Record<string, unknown>),
        disabled = false,
        title = "Audience Builder",
    } = $props<{
        audienceSourceType?: string;
        assignmentRuleType?: string;
        participants?: AudienceParticipant[];
        ruleConfig?: Record<string, unknown>;
        disabled?: boolean;
        title?: string;
    }>();

    const roleOptions = ["SELF", "PEER", "SUPERVISOR", "SUBORDINATE", "EXTERNAL"];

    $effect(() => {
        if (!ruleConfig || Object.keys(ruleConfig).length === 0) {
            ruleConfig = defaultRuleConfig(assignmentRuleType);
        }
    });

    function addParticipant() {
        participants = [...participants, { userId: "", supervisorId: "", department: "" }];
    }

    function removeParticipant(index: number) {
        participants = participants.filter((_: AudienceParticipant, i: number) => i !== index);
    }

    function updateParticipant(index: number, patch: Partial<AudienceParticipant>) {
        participants = participants.map((p: AudienceParticipant, i: number) =>
            i === index ? { ...p, ...patch } : p,
        );
    }

    function setRuleType(value: string) {
        assignmentRuleType = value;
        ruleConfig = defaultRuleConfig(value);
    }

    function setRulePatch(patch: Record<string, unknown>) {
        ruleConfig = { ...ruleConfig, ...patch };
    }

    function getString(key: string, fallback = ""): string {
        const value = ruleConfig?.[key];
        return value === undefined || value === null ? fallback : String(value);
    }

    function getBool(key: string, fallback: boolean): boolean {
        const value = ruleConfig?.[key];
        return typeof value === "boolean" ? value : fallback;
    }

    function getNumber(key: string, fallback: number): number {
        const value = ruleConfig?.[key];
        if (typeof value === "number") {
            return value;
        }
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : fallback;
    }
</script>

<div class="rounded-md border p-4 space-y-4">
    <div class="flex items-center gap-2">
        <Users class="h-4 w-4" />
        <h2 class="font-semibold">{title}</h2>
    </div>

    <div class="grid gap-3 md:grid-cols-2">
        <div class="grid gap-2">
            <Label for="ab-source-type">Audience Source Type</Label>
            <select
                id="ab-source-type"
                bind:value={audienceSourceType}
                class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                disabled={disabled}
            >
                {#each AUDIENCE_SOURCE_TYPES as type}
                    <option value={type}>{type}</option>
                {/each}
            </select>
        </div>

        <div class="grid gap-2">
            <Label for="ab-rule-type">Assignment Rule Type</Label>
            <select
                id="ab-rule-type"
                value={assignmentRuleType}
                onchange={(e) => setRuleType((e.currentTarget as HTMLSelectElement).value)}
                class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                disabled={disabled}
            >
                {#each ASSIGNMENT_RULES as type}
                    <option value={type}>{type}</option>
                {/each}
            </select>
        </div>
    </div>

    <div class="space-y-3">
        <div class="flex items-center justify-between">
            <h3 class="text-sm font-medium">Participants</h3>
            <Button type="button" size="sm" variant="outline" onclick={addParticipant} disabled={disabled}>
                <Plus class="mr-2 h-4 w-4" /> Add
            </Button>
        </div>

        {#if participants.length === 0}
            <div class="rounded-md border border-dashed p-3 text-sm text-muted-foreground">
                No participants yet. Add users to build audience mappings.
            </div>
        {:else}
            <div class="space-y-2">
                {#each participants as participant, index (index)}
                    <div class="grid gap-2 rounded-md border p-3 md:grid-cols-[1.2fr_1fr_1fr_auto]">
                        <Input
                            placeholder="User ID"
                            value={participant.userId}
                            oninput={(e) =>
                                updateParticipant(index, {
                                    userId: (e.currentTarget as HTMLInputElement).value,
                                })}
                            disabled={disabled}
                        />
                        <Input
                            placeholder="Supervisor ID (optional)"
                            value={participant.supervisorId || ""}
                            oninput={(e) =>
                                updateParticipant(index, {
                                    supervisorId: (e.currentTarget as HTMLInputElement).value,
                                })}
                            disabled={disabled}
                        />
                        <Input
                            placeholder="Department (optional)"
                            value={participant.department || ""}
                            oninput={(e) =>
                                updateParticipant(index, {
                                    department: (e.currentTarget as HTMLInputElement).value,
                                })}
                            disabled={disabled}
                        />
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            onclick={() => removeParticipant(index)}
                            disabled={disabled}
                        >
                            <Trash2 class="h-4 w-4" />
                        </Button>
                    </div>
                {/each}
            </div>
        {/if}
    </div>

    <div class="space-y-3">
        <h3 class="text-sm font-medium">Rule Settings</h3>

        <div class="grid gap-3 md:grid-cols-2">
            <div class="grid gap-2">
                <Label for="ab-role">Evaluator Role</Label>
                <select
                    id="ab-role"
                    value={getString("evaluatorRole", "PEER")}
                    onchange={(e) =>
                        setRulePatch({ evaluatorRole: (e.currentTarget as HTMLSelectElement).value })}
                    class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    disabled={disabled}
                >
                    {#each roleOptions as role}
                        <option value={role}>{role}</option>
                    {/each}
                </select>
            </div>

            {#if assignmentRuleType === "ATTRIBUTE_MATCH"}
                <div class="grid gap-2">
                    <Label for="ab-match-attr">Match Attribute</Label>
                    <Input
                        id="ab-match-attr"
                        placeholder="department"
                        value={getString("matchAttribute", "department")}
                        oninput={(e) =>
                            setRulePatch({
                                matchAttribute: (e.currentTarget as HTMLInputElement).value,
                            })}
                        disabled={disabled}
                    />
                </div>
            {/if}

            {#if assignmentRuleType === "ROUND_ROBIN"}
                <div class="grid gap-2">
                    <Label for="ab-eval-per-evaluatee">Evaluators per Evaluatee</Label>
                    <Input
                        id="ab-eval-per-evaluatee"
                        type="number"
                        min="1"
                        value={getNumber("evaluatorsPerEvaluatee", 2)}
                        oninput={(e) =>
                            setRulePatch({
                                evaluatorsPerEvaluatee: Math.max(
                                    1,
                                    Number((e.currentTarget as HTMLInputElement).value || 1),
                                ),
                            })}
                        disabled={disabled}
                    />
                </div>
            {/if}

            {#if assignmentRuleType === "ALL_TO_ALL" || assignmentRuleType === "ATTRIBUTE_MATCH"}
                <div class="grid gap-2">
                    <Label for="ab-max-per-evaluatee">Max Evaluators per Evaluatee</Label>
                    <Input
                        id="ab-max-per-evaluatee"
                        type="number"
                        min="1"
                        value={getNumber("maxEvaluatorsPerEvaluatee", 3)}
                        oninput={(e) =>
                            setRulePatch({
                                maxEvaluatorsPerEvaluatee: Math.max(
                                    1,
                                    Number((e.currentTarget as HTMLInputElement).value || 1),
                                ),
                            })}
                        disabled={disabled}
                    />
                </div>
            {/if}
        </div>

        <div class="grid gap-2 md:grid-cols-2">
            {#if assignmentRuleType === "ALL_TO_ALL" || assignmentRuleType === "ROUND_ROBIN" || assignmentRuleType === "ATTRIBUTE_MATCH"}
                <div class="flex items-center space-x-2">
                    <Checkbox
                        id="ab-allow-self"
                        checked={getBool("allowSelfEvaluation", false)}
                        onCheckedChange={(v) => setRulePatch({ allowSelfEvaluation: v })}
                        disabled={disabled}
                    />
                    <Label for="ab-allow-self">Allow self evaluation</Label>
                </div>
            {/if}

            {#if assignmentRuleType === "MANAGER_HIERARCHY"}
                <div class="flex items-center space-x-2">
                    <Checkbox
                        id="ab-include-self"
                        checked={getBool("includeSelfEvaluation", false)}
                        onCheckedChange={(v) => setRulePatch({ includeSelfEvaluation: v })}
                        disabled={disabled}
                    />
                    <Label for="ab-include-self">Include self evaluation</Label>
                </div>
                <div class="flex items-center space-x-2">
                    <Checkbox
                        id="ab-require-known"
                        checked={getBool("requireKnownManager", true)}
                        onCheckedChange={(v) => setRulePatch({ requireKnownManager: v })}
                        disabled={disabled}
                    />
                    <Label for="ab-require-known">Require manager in audience list</Label>
                </div>
            {/if}
        </div>
    </div>
</div>
