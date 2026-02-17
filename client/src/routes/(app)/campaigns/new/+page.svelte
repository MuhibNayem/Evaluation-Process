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

    let templates = $state<any[]>([]);
    let isLoadingTemplates = $state(true);
    let isSubmitting = $state(false);
    let error = $state<string | null>(null);

    let formData = $state({
        name: "",
        description: "",
        templateId: "",
        templateVersion: 1, // Default, will update when template selected
        startDate: "",
        endDate: "",
        scoringMethod: "WEIGHTED_AVERAGE",
        anonymousMode: false,
        minimumRespondents: 1,
    });

    async function fetchTemplates() {
        try {
            const response = await api.get("/templates"); // Assuming this endpoint exists
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
            // Convert dates to ISO Instant format (e.g., "2023-10-27T10:00:00Z")
            // Simple approach: append T00:00:00Z for start, T23:59:59Z for end
            const payload = {
                ...formData,
                startDate: new Date(formData.startDate).toISOString(),
                endDate: new Date(formData.endDate).toISOString(),
                anonymousRoles: [], // Default empty for now
            };

            await api.post("/campaigns", payload);
            goto("/campaigns");
        } catch (err: any) {
            console.error("Failed to create campaign:", err);
            error = err.response?.data?.detail || "Failed to create campaign.";
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

    <form onsubmit={handleSubmit} class="grid gap-6 max-w-2xl">
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
                        <div class="p-2 text-center text-sm">
                            Loading templates...
                        </div>
                    {:else if templates.length === 0}
                        <div class="p-2 text-center text-sm">
                            No templates available
                        </div>
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

        <div class="grid gap-2">
            <Label for="scoring">Scoring Method</Label>
            <Select.Root
                type="single"
                value={formData.scoringMethod}
                onValueChange={(v) => (formData.scoringMethod = v)}
            >
                <Select.Trigger>
                    {formData.scoringMethod}
                </Select.Trigger>
                <Select.Content>
                    <Select.Item value="WEIGHTED_AVERAGE"
                        >Weighted Average</Select.Item
                    >
                    <Select.Item value="SIMPLE_AVERAGE"
                        >Simple Average</Select.Item
                    >
                </Select.Content>
            </Select.Root>
        </div>

        <div class="flex items-center space-x-2">
            <Checkbox
                id="anonymous"
                checked={formData.anonymousMode}
                onCheckedChange={(v) => (formData.anonymousMode = v)}
            />
            <Label for="anonymous">Anonymous Mode</Label>
        </div>

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
