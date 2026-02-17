<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import {
        Select,
        SelectContent,
        SelectItem,
        SelectTrigger,
    } from "$lib/components/ui/select/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Loader2, FileDown, FileText } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let campaigns = $state<any[]>([]);
    let selectedCampaignId = $state<string>("");
    let evaluatees = $state<string[]>([]);
    let selectedEvaluateeId = $state<string>("");

    let loadingCampaigns = $state(true);
    let loadingEvaluatees = $state(false);
    let downloadingCsv = $state(false);
    let downloadingPdf = $state(false);

    onMount(async () => {
        try {
            const res = await api.get("/campaigns");
            campaigns = res.data;
        } catch (err) {
            console.error(err);
            toast.error("Failed to load campaigns");
        } finally {
            loadingCampaigns = false;
        }
    });

    async function handleCampaignChange(value: string) {
        selectedCampaignId = value;
        selectedEvaluateeId = "";
        evaluatees = [];

        if (!value) return;

        loadingEvaluatees = true;
        try {
            // Fetch evaluations to get unique evaluatees
            // Note: This might be inefficient for large campaigns, better to have a dedicated endpoint
            const res = await api.get(`/evaluations/campaign/${value}`);
            const evals = res.data;
            const uniqueEvaluatees = new Set<string>();
            evals.forEach((e: any) => {
                if (e.evaluateeId) uniqueEvaluatees.add(e.evaluateeId);
            });
            evaluatees = Array.from(uniqueEvaluatees).sort();
        } catch (err) {
            console.error(err);
            toast.error("Failed to load evaluatees");
        } finally {
            loadingEvaluatees = false;
        }
    }

    async function downloadCampaignCsv() {
        if (!selectedCampaignId) return;
        downloadingCsv = true;
        try {
            const res = await api.get(
                `/reports/export/csv/${selectedCampaignId}`,
                {
                    responseType: "blob",
                },
            );
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement("a");
            link.href = url;
            link.setAttribute("download", `campaign-${selectedCampaignId}.csv`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            console.error(err);
            toast.error("Failed to download CSV report");
        } finally {
            downloadingCsv = false;
        }
    }

    async function downloadIndividualPdf() {
        if (!selectedCampaignId || !selectedEvaluateeId) return;
        downloadingPdf = true;
        try {
            const res = await api.get(`/reports/export/pdf`, {
                params: {
                    campaignId: selectedCampaignId,
                    evaluateeId: selectedEvaluateeId,
                },
                responseType: "blob",
            });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement("a");
            link.href = url;
            link.setAttribute(
                "download",
                `report-${selectedEvaluateeId}-${selectedCampaignId}.pdf`,
            );
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            console.error(err);
            toast.error("Failed to download PDF report");
        } finally {
            downloadingPdf = false;
        }
    }
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">Reports</h1>
        <p class="text-muted-foreground">
            Generate and export evaluation reports.
        </p>
    </div>

    <div class="grid gap-6 md:grid-cols-2">
        <Card.Root>
            <Card.Header>
                <Card.Title>Campaign Report</Card.Title>
                <Card.Description>
                    Export all evaluation data for a specific campaign as CSV.
                </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-4">
                <div class="space-y-2">
                    <label for="campaign-select" class="text-sm font-medium"
                        >Select Campaign</label
                    >
                    {#if loadingCampaigns}
                        <div class="flex items-center gap-2 text-sm text-muted">
                            <Loader2 class="h-4 w-4 animate-spin" /> Loading...
                        </div>
                    {:else}
                        <Select
                            type="single"
                            value={selectedCampaignId}
                            onValueChange={handleCampaignChange}
                        >
                            <SelectTrigger>
                                {campaigns.find(
                                    (c) => c.id === selectedCampaignId,
                                )?.name || "Select a campaign"}
                            </SelectTrigger>
                            <SelectContent>
                                {#each campaigns as campaign}
                                    <SelectItem value={campaign.id}
                                        >{campaign.name}</SelectItem
                                    >
                                {/each}
                            </SelectContent>
                        </Select>
                    {/if}
                </div>
            </Card.Content>
            <Card.Footer>
                <Button
                    onclick={downloadCampaignCsv}
                    disabled={!selectedCampaignId || downloadingCsv}
                >
                    {#if downloadingCsv}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {:else}
                        <FileDown class="mr-2 h-4 w-4" />
                    {/if}
                    Download CSV
                </Button>
            </Card.Footer>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <Card.Title>Individual Report</Card.Title>
                <Card.Description>
                    Generate a PDF report for a specific evaluatee in a
                    campaign.
                </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-4">
                <div class="space-y-2">
                    <label for="evaluatee-select" class="text-sm font-medium"
                        >Select Evaluatee</label
                    >

                    {#if !selectedCampaignId}
                        <div
                            class="text-sm text-muted-foreground italic border p-2 rounded bg-muted/50"
                        >
                            Please select a campaign first.
                        </div>
                    {:else if loadingEvaluatees}
                        <div class="flex items-center gap-2 text-sm text-muted">
                            <Loader2 class="h-4 w-4 animate-spin" /> Loading evaluatees...
                        </div>
                    {:else if evaluatees.length === 0}
                        <div class="text-sm text-muted-foreground">
                            No evaluatees found for this campaign.
                        </div>
                    {:else}
                        <Select
                            type="single"
                            value={selectedEvaluateeId}
                            onValueChange={(v) => (selectedEvaluateeId = v)}
                        >
                            <SelectTrigger>
                                {selectedEvaluateeId || "Select an evaluatee"}
                            </SelectTrigger>
                            <SelectContent class="max-h-[200px]">
                                {#each evaluatees as evaluatee}
                                    <SelectItem value={evaluatee}
                                        >{evaluatee}</SelectItem
                                    >
                                {/each}
                            </SelectContent>
                        </Select>
                    {/if}
                </div>
            </Card.Content>
            <Card.Footer>
                <Button
                    onclick={downloadIndividualPdf}
                    disabled={!selectedCampaignId ||
                        !selectedEvaluateeId ||
                        downloadingPdf}
                    variant="outline"
                >
                    {#if downloadingPdf}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {:else}
                        <FileText class="mr-2 h-4 w-4" />
                    {/if}
                    Download PDF
                </Button>
            </Card.Footer>
        </Card.Root>
    </div>
</div>
