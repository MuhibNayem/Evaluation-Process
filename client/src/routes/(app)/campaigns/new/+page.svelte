<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { goto } from "$app/navigation";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import DatePicker from "$lib/components/ui/date-picker/date-picker.svelte";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import * as Select from "$lib/components/ui/select/index.js";
    import { Loader2, ArrowLeft } from "@lucide/svelte";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import AudienceBuilder from "$lib/components/campaign/AudienceBuilder.svelte";
    import {
        defaultRuleConfig,
        normalizeRuleConfig,
        sourceConfigFromParticipants,
        type AudienceParticipant,
    } from "$lib/campaign/audience-builder.js";

    let templates = $state<any[]>([]);
    let isLoadingTemplates = $state(true);
    let isSubmitting = $state(false);
    let error = $state<string | null>(null);

    let formData = $state({
        name: "",
        description: "",
        templateId: "",
        templateVersion: 1,
        startDate: "",
        endDate: "",
        scoringMethod: "WEIGHTED_AVERAGE",
        anonymousMode: false,
        minimumRespondents: 1,
    });

    let enableDynamicAssignments = $state(false);
    let audienceSourceType = $state("INLINE");
    let assignmentRuleType = $state("ATTRIBUTE_MATCH");
    let audienceParticipants = $state<AudienceParticipant[]>([]);
    let assignmentRuleConfig = $state<Record<string, unknown>>(defaultRuleConfig("ATTRIBUTE_MATCH"));

    async function fetchTemplates() {
        try {
            const response = await api.get("/templates");
            templates = response.data;
        } catch (err) {
            console.error("Failed to fetch templates:", err);
            error = "Failed to load templates. Please try again.";
        } finally {
            isLoadingTemplates = false;
        }
    }

    onMount(() => {
        fetchTemplates();
    });

    function handleTemplateChange(value: string) {
        formData.templateId = value;
        const selectedTemplate = templates.find((t) => t.id === value);
        if (selectedTemplate) {
            formData.templateVersion = selectedTemplate.currentVersion;
        }
    }

    async function handleSubmit(e: Event) {
        e.preventDefault();
        isSubmitting = true;
        error = null;

        try {
            const payload: Record<string, unknown> = {
                name: formData.name,
                description: formData.description,
                templateId: formData.templateId,
                templateVersion: formData.templateVersion,
                startDate: new Date(formData.startDate).toISOString(),
                endDate: new Date(formData.endDate).toISOString(),
                scoringMethod: formData.scoringMethod,
                anonymousMode: formData.anonymousMode,
                anonymousRoles: [],
                minimumRespondents: Number(formData.minimumRespondents),
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

            await api.post("/campaigns", payload);
            goto("/campaigns");
        } catch (err: any) {
            console.error("Failed to create campaign:", err);
            error = err.response?.data?.detail || err.message || "Failed to create campaign.";
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="flex flex-col gap-6">
    <div class="flex items-center gap-4">
        <Button
            variant="ghost"
            size="icon"
            href="/campaigns"
            onclick={() => goto("/campaigns")}
        >
            <ArrowLeft class="h-4 w-4" />
        </Button>
        <h1 class="text-lg font-semibold md:text-2xl">Create Campaign</h1>
    </div>

    <form onsubmit={handleSubmit} class="grid gap-6 max-w-4xl">
        {#if error}
            <div
                class="rounded-md bg-red-50 p-4 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-400"
            >
                {error}
            </div>
        {/if}

        <div class="grid gap-2">
            <Label for="name">Campaign Name</Label>
            <Input
                id="name"
                bind:value={formData.name}
                required
                placeholder="e.g., Q1 Performance Review"
            />
        </div>

        <div class="grid gap-2">
            <Label for="description">Description</Label>
            <Textarea
                id="description"
                bind:value={formData.description}
                placeholder="Describe the purpose of this campaign..."
            />
        </div>

        <div class="grid grid-cols-2 gap-4">
            <div class="grid gap-2">
                <Label for="startDate">Start Date</Label>
                <DatePicker bind:value={formData.startDate} />
            </div>
            <div class="grid gap-2">
                <Label for="endDate">End Date</Label>
                <DatePicker bind:value={formData.endDate} />
            </div>
        </div>

        <div class="grid gap-2">
            <Label for="template">Evaluation Template</Label>
            <Select.Root type="single" onValueChange={handleTemplateChange}>
                <Select.Trigger>
                    {templates.find((t) => t.id === formData.templateId)
                        ?.name || "Select a template"}
                </Select.Trigger>
                <Select.Content>
                    {#if isLoadingTemplates}
                        <div class="p-2 text-center text-sm">Loading templates...</div>
                    {:else if templates.length === 0}
                        <div class="p-2 text-center text-sm">No templates available</div>
                    {:else}
                        {#each templates as template}
                            <Select.Item value={template.id}
                                >{template.name} (v{template.currentVersion})</Select.Item
                            >
                        {/each}
                    {/if}
                </Select.Content>
            </Select.Root>
        </div>

        <div class="grid grid-cols-2 gap-4">
            <div class="grid gap-2">
                <Label for="scoringMethod">Scoring Method</Label>
                <select
                    id="scoringMethod"
                    bind:value={formData.scoringMethod}
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
                <Label for="minimumRespondents">Min Respondents</Label>
                <Input
                    id="minimumRespondents"
                    type="number"
                    min="1"
                    bind:value={formData.minimumRespondents}
                />
            </div>
        </div>

        <div class="flex items-center space-x-2">
            <Checkbox
                id="anonymous"
                checked={formData.anonymousMode}
                onCheckedChange={(v) => (formData.anonymousMode = v)}
            />
            <Label for="anonymous">Anonymous Mode</Label>
        </div>

        <div class="flex items-center space-x-2">
            <Checkbox
                id="enable-dynamic"
                checked={enableDynamicAssignments}
                onCheckedChange={(v) => (enableDynamicAssignments = v)}
            />
            <Label for="enable-dynamic">Enable dynamic audience assignment</Label>
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

        <div class="flex justify-end gap-4">
            <Button
                variant="outline"
                type="button"
                onclick={() => goto("/campaigns")}>Cancel</Button
            >
            <Button type="submit" disabled={isSubmitting}>
                {#if isSubmitting}
                    <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    Creating...
                {:else}
                    Create Campaign
                {/if}
            </Button>
        </div>
    </form>
</div>
