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
    import * as Select from "$lib/components/ui/select/index.js";
    import { toast } from "svelte-sonner";
    import { Loader2, ArrowLeft } from "lucide-svelte";
    import {
        DateFormatter,
        type DateValue,
        getLocalTimeZone,
        parseDate,
        today,
    } from "@internationalized/date";

    // Since we don't have a date picker component setup yet in the plan, I'll use simple HTML date inputs for now or text inputs.
    // Actually, Shadcn has a Calendar but setting it up might be complex in one go.
    // I'll use standard <input type="datetime-local" /> for simplicity and robustness.

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

    // We can't easily change Template once created in this simple version
    let templateName = $state("");

    async function fetchCampaign() {
        loading = true;
        try {
            const res = await api.get(`/campaigns/${campaignId}`);
            const c = res.data;
            name = c.name;
            description = c.description || "";
            // Convert ISO string to datetime-local format (YYYY-MM-DDTHH:mm)
            startDate = c.startDate
                ? new Date(c.startDate).toISOString().slice(0, 16)
                : "";
            endDate = c.endDate
                ? new Date(c.endDate).toISOString().slice(0, 16)
                : "";
            scoringMethod = c.scoringMethod;
            anonymousMode = c.anonymousMode;
            minimumRespondents = c.minimumRespondents || 1;
            // Fetch template name if possible, or just ignore
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
            const payload = {
                name,
                description,
                startDate: new Date(startDate).toISOString(),
                endDate: new Date(endDate).toISOString(),
                scoringMethod,
                anonymousMode,
                anonymousRoles: [], // Default empty for now
                minimumRespondents: Number(minimumRespondents),
            };

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

<div class="mx-auto max-w-2xl pb-20">
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
                    <Input
                        type="datetime-local"
                        id="startDate"
                        bind:value={startDate}
                        required
                    />
                </div>
                <div class="grid gap-2">
                    <Label for="endDate">End Date</Label>
                    <Input
                        type="datetime-local"
                        id="endDate"
                        bind:value={endDate}
                        required
                    />
                </div>
            </div>

            <div class="grid grid-cols-2 gap-4">
                <div class="grid gap-2">
                    <Label for="scoring">Scoring Method</Label>
                    <select
                        id="scoring"
                        bind:value={scoringMethod}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        <option value="WEIGHTED_AVERAGE"
                            >Weighted Average</option
                        >
                        <option value="SIMPLE_AVERAGE">Simple Average</option>
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
                <Checkbox id="anonymous" bind:checked={anonymousMode} />
                <Label for="anonymous" class="font-normal"
                    >Enable Anonymous Mode</Label
                >
            </div>

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
