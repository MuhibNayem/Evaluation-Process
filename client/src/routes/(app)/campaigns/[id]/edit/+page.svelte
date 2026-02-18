<script lang="ts">
    import { page } from "$app/stores";
    import { goto } from "$app/navigation";
    import api from "$lib/api.js";
    import { onMount } from "svelte";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import { toast } from "svelte-sonner";
    import DatePicker from "$lib/components/ui/date-picker/date-picker.svelte";
    import { Loader2, ArrowLeft } from "@lucide/svelte";
    import AudienceBuilder from "$lib/components/campaign/AudienceBuilder.svelte";
    import {
        defaultRuleConfig,
        normalizeRuleConfig,
        participantsFromSourceConfig,
        sourceConfigFromParticipants,
        type AudienceParticipant,
    } from "$lib/campaign/audience-builder.js";

    let campaignId = $state($page.params.id);
    let loading = $state(true);
    let saving = $state(false);

    let name = $state("");
    let description = $state("");
    let startDate = $state("");
    let endDate = $state("");
    let scoringMethod = $state("WEIGHTED_AVERAGE");
    let anonymousMode = $state(false);
    let minimumRespondents = $state(1);

    let enableDynamicAssignments = $state(false);
    let audienceSourceType = $state("INLINE");
    let assignmentRuleType = $state("ATTRIBUTE_MATCH");
    let audienceParticipants = $state<AudienceParticipant[]>([]);
    let assignmentRuleConfig = $state<Record<string, unknown>>(defaultRuleConfig("ATTRIBUTE_MATCH"));

    async function fetchCampaign() {
        loading = true;
        try {
            const res = await api.get(`/campaigns/${campaignId}`);
            const c = res.data;
            name = c.name;
            description = c.description || "";
            startDate = c.startDate ? new Date(c.startDate).toISOString().slice(0, 10) : "";
            endDate = c.endDate ? new Date(c.endDate).toISOString().slice(0, 10) : "";
            scoringMethod = c.scoringMethod;
            anonymousMode = c.anonymousMode;
            minimumRespondents = c.minimumRespondents || 1;

            if (c.audienceSourceType || c.assignmentRuleType) {
                enableDynamicAssignments = true;
                audienceSourceType = c.audienceSourceType || "INLINE";
                assignmentRuleType = c.assignmentRuleType || "ATTRIBUTE_MATCH";
                audienceParticipants = participantsFromSourceConfig(c.audienceSourceConfig);
                assignmentRuleConfig = normalizeRuleConfig(
                    assignmentRuleType,
                    c.assignmentRuleConfig,
                );
            }
        } catch (err) {
            console.error(err);
            toast.error("Failed to load campaign");
            goto("/campaigns");
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchCampaign();
    });

    async function handleSubmit(e: Event) {
        e.preventDefault();
        saving = true;

        try {
            const payload: Record<string, unknown> = {
                name,
                description,
                startDate: new Date(startDate).toISOString(),
                endDate: new Date(endDate).toISOString(),
                scoringMethod,
                anonymousMode,
                anonymousRoles: [],
                minimumRespondents: Number(minimumRespondents),
            };

            if (enableDynamicAssignments) {
                payload.audienceSourceType = audienceSourceType;
                payload.audienceSourceConfig = sourceConfigFromParticipants(audienceParticipants);
                payload.assignmentRuleType = assignmentRuleType;
                payload.assignmentRuleConfig = normalizeRuleConfig(
                    assignmentRuleType,
                    assignmentRuleConfig,
                );
            }

            await api.put(`/campaigns/${campaignId}`, payload);
            toast.success("Campaign updated successfully");
            goto(`/campaigns/${campaignId}`);
        } catch (err: any) {
            console.error(err);
            toast.error(
                "Failed to update: " +
                    (err.response?.data?.detail || err.message),
            );
        } finally {
            saving = false;
        }
    }
</script>

<div class="mx-auto max-w-4xl pb-20">
    <div class="mb-6 flex items-center gap-4">
        <Button
            variant="ghost"
            size="icon"
            onclick={() => goto(`/campaigns/${campaignId}`)}
        >
            <ArrowLeft class="h-4 w-4" />
        </Button>
        <h1 class="text-2xl font-bold">Edit Campaign</h1>
    </div>

    {#if loading}
        <div class="flex h-40 items-center justify-center">
            <Loader2 class="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
    {:else}
        <form onsubmit={handleSubmit} class="space-y-6">
            <div class="grid gap-2">
                <Label for="name">Campaign Name</Label>
                <Input
                    id="name"
                    bind:value={name}
                    required
                    placeholder="e.g. Q1 Performance Review"
                />
            </div>

            <div class="grid gap-2">
                <Label for="description">Description</Label>
                <Textarea
                    id="description"
                    bind:value={description}
                    placeholder="Optional description..."
                />
            </div>

            <div class="grid grid-cols-2 gap-4">
                <div class="grid gap-2">
                    <Label for="startDate">Start Date</Label>
                    <DatePicker bind:value={startDate} />
                </div>
                <div class="grid gap-2">
                    <Label for="endDate">End Date</Label>
                    <DatePicker bind:value={endDate} />
                </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
                <div class="grid gap-2">
                    <Label for="scoring">Scoring Method</Label>
                    <select
                        id="scoring"
                        bind:value={scoringMethod}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        <option value="WEIGHTED_AVERAGE">Weighted Average</option>
                        <option value="SIMPLE_AVERAGE">Simple Average</option>
                        <option value="MEDIAN">Median</option>
                        <option value="PERCENTILE_RANK">Percentile Rank</option>
                        <option value="CUSTOM_FORMULA">Custom Formula</option>
                    </select>
                </div>
                <div class="grid gap-2">
                    <Label for="minRespondents">Min Respondents</Label>
                    <Input
                        type="number"
                        id="minRespondents"
                        bind:value={minimumRespondents}
                        min="1"
                    />
                </div>
            </div>

            <div class="flex items-center space-x-2">
                <Checkbox
                    id="anonymous"
                    checked={anonymousMode}
                    onCheckedChange={(v) => (anonymousMode = v)}
                />
                <Label for="anonymous" class="font-normal"
                    >Enable Anonymous Mode</Label
                >
            </div>

            <div class="flex items-center space-x-2">
                <Checkbox
                    id="enable-dynamic-edit"
                    checked={enableDynamicAssignments}
                    onCheckedChange={(v) => (enableDynamicAssignments = v)}
                />
                <Label for="enable-dynamic-edit">Enable dynamic audience assignment</Label>
            </div>

            {#if enableDynamicAssignments}
                <AudienceBuilder
                    bind:audienceSourceType
                    bind:assignmentRuleType
                    bind:participants={audienceParticipants}
                    bind:ruleConfig={assignmentRuleConfig}
                    title="Audience Builder"
                />
            {/if}

            <div class="flex justify-end gap-4 pt-4">
                <Button
                    type="button"
                    variant="outline"
                    onclick={() => goto(`/campaigns/${campaignId}`)}
                    >Cancel</Button
                >
                <Button type="submit" disabled={saving}>
                    {#if saving}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {/if}
                    Save Changes
                </Button>
            </div>
        </form>
    {/if}
</div>
