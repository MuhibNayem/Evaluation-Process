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
    import * as Dialog from "$lib/components/ui/dialog/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import DataView from "$lib/components/data-view.svelte";
    import { Loader2, FileDown, FileText } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let campaigns = $state<any[]>([]);
    let selectedCampaignId = $state<string>("");
    let evaluatees = $state<string[]>([]);
    let selectedEvaluateeId = $state<string>("");
    let campaignEvaluations = $state<any[]>([]);
    let selectedEvaluation = $state<any>(null);
    let selectedEvaluationTemplate = $state<any>(null);
    let responseDialogOpen = $state(false);
    let loadingResponseTemplate = $state(false);
    let templateCache = $state<Record<string, any>>({});

    let loadingCampaigns = $state(true);
    let loadingEvaluatees = $state(false);
    let loadingCampaignEvaluations = $state(false);
    let downloadingCsv = $state(false);
    let downloadingPdf = $state(false);
    let loadingCampaignReport = $state(false);
    let loadingIndividualReport = $state(false);
    let campaignReport = $state<any>(null);
    let individualReport = $state<any>(null);

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
        campaignEvaluations = [];
        selectedEvaluation = null;

        if (!value) return;

        loadingEvaluatees = true;
        loadingCampaignEvaluations = true;
        try {
            // Fetch evaluations to get unique evaluatees
            // Note: This might be inefficient for large campaigns, better to have a dedicated endpoint
            const res = await api.get(`/evaluations/campaign/${value}`);
            const evals = res.data;
            campaignEvaluations = evals;
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
            loadingCampaignEvaluations = false;
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

    async function fetchCampaignReport() {
        if (!selectedCampaignId) return;
        loadingCampaignReport = true;
        try {
            const res = await api.get(`/reports/campaign/${selectedCampaignId}`);
            campaignReport = res.data;
            toast.success("Campaign report loaded");
        } catch (err) {
            console.error(err);
            toast.error("Failed to load campaign report");
        } finally {
            loadingCampaignReport = false;
        }
    }

    async function fetchIndividualReport() {
        if (!selectedCampaignId || !selectedEvaluateeId) return;
        loadingIndividualReport = true;
        try {
            const res = await api.get("/reports/individual", {
                params: {
                    campaignId: selectedCampaignId,
                    evaluateeId: selectedEvaluateeId,
                },
            });
            individualReport = res.data;
            toast.success("Individual report loaded");
        } catch (err) {
            console.error(err);
            toast.error("Failed to load individual report");
        } finally {
            loadingIndividualReport = false;
        }
    }

    async function openEvaluationViewer(ev: any) {
        selectedEvaluation = ev;
        responseDialogOpen = true;
        selectedEvaluationTemplate = null;

        const templateId = ev?.templateId;
        if (!templateId) return;

        if (templateCache[templateId]) {
            selectedEvaluationTemplate = templateCache[templateId];
            return;
        }

        loadingResponseTemplate = true;
        try {
            const res = await api.get(`/templates/${templateId}`);
            templateCache = { ...templateCache, [templateId]: res.data };
            selectedEvaluationTemplate = res.data;
        } catch (err) {
            console.error(err);
            toast.error("Failed to load template for response viewer");
        } finally {
            loadingResponseTemplate = false;
        }
    }

    function findAnswer(questionId: string) {
        return selectedEvaluation?.answers?.find((a: any) => a.questionId === questionId) ?? null;
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
                <Button
                    variant="outline"
                    onclick={fetchCampaignReport}
                    disabled={!selectedCampaignId || loadingCampaignReport}
                >
                    {#if loadingCampaignReport}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {/if}
                    View Campaign Report
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
                <Button
                    variant="outline"
                    onclick={fetchIndividualReport}
                    disabled={!selectedCampaignId ||
                        !selectedEvaluateeId ||
                        loadingIndividualReport}
                >
                    {#if loadingIndividualReport}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {/if}
                    View Individual Report
                </Button>
            </Card.Footer>
        </Card.Root>
    </div>

    {#if campaignReport}
        <Card.Root>
            <Card.Header>
                <Card.Title>Campaign Report</Card.Title>
            </Card.Header>
            <Card.Content>
                <div class="rounded-md border bg-muted/30 p-3">
                    <DataView data={campaignReport} />
                </div>
            </Card.Content>
        </Card.Root>
    {/if}

    {#if individualReport}
        <Card.Root>
            <Card.Header>
                <Card.Title>Individual Report</Card.Title>
            </Card.Header>
            <Card.Content>
                <div class="rounded-md border bg-muted/30 p-3">
                    <DataView data={individualReport} />
                </div>
            </Card.Content>
        </Card.Root>
    {/if}

    <Card.Root>
        <Card.Header>
            <Card.Title>Evaluation Responses (Admin)</Card.Title>
            <Card.Description>
                Campaign-level raw responses submitted by evaluators.
            </Card.Description>
        </Card.Header>
        <Card.Content class="space-y-3">
            {#if !selectedCampaignId}
                <p class="text-sm text-muted-foreground">
                    Select a campaign first to view responses.
                </p>
            {:else if loadingCampaignEvaluations}
                <div class="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 class="h-4 w-4 animate-spin" /> Loading responses...
                </div>
            {:else if campaignEvaluations.length === 0}
                <p class="text-sm text-muted-foreground">
                    No responses found for this campaign.
                </p>
            {:else}
                <div class="space-y-2">
                    {#each campaignEvaluations as ev}
                        <div class="rounded border p-3 text-sm">
                            <div class="flex items-center justify-between gap-2">
                                <div class="font-medium">
                                    Eval #{ev.id}
                                </div>
                                <div class="text-xs text-muted-foreground">
                                    {ev.status}
                                </div>
                            </div>
                            <div class="mt-1 text-xs text-muted-foreground">
                                evaluator: {ev.evaluatorId} | evaluatee: {ev.evaluateeId} | answers: {ev.answerCount}
                            </div>
                            <div class="mt-2 flex items-center gap-2">
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onclick={() => openEvaluationViewer(ev)}
                                >
                                    View Response
                                </Button>
                            </div>
                        </div>
                    {/each}
                </div>
            {/if}
        </Card.Content>
    </Card.Root>
</div>

<Dialog.Root bind:open={responseDialogOpen}>
    <Dialog.Content class="max-h-[85vh] max-w-4xl overflow-auto">
        <Dialog.Header>
            <Dialog.Title>Submitted Response</Dialog.Title>
            <Dialog.Description>
                Read-only response view with original question text.
            </Dialog.Description>
        </Dialog.Header>

        {#if !selectedEvaluation}
            <p class="text-sm text-muted-foreground">No evaluation selected.</p>
        {:else}
            <div class="space-y-3">
                <div class="grid gap-1 text-sm">
                    <div><span class="font-medium">Evaluation ID:</span> {selectedEvaluation.id}</div>
                    <div><span class="font-medium">Evaluator:</span> {selectedEvaluation.evaluatorId}</div>
                    <div><span class="font-medium">Evaluatee:</span> {selectedEvaluation.evaluateeId}</div>
                    <div><span class="font-medium">Status:</span> {selectedEvaluation.status}</div>
                    <div><span class="font-medium">Answer Count:</span> {selectedEvaluation.answerCount}</div>
                </div>

                {#if loadingResponseTemplate}
                    <div class="flex items-center gap-2 text-sm text-muted-foreground">
                        <Loader2 class="h-4 w-4 animate-spin" /> Loading form layout...
                    </div>
                {:else if !selectedEvaluationTemplate}
                    <p class="text-sm text-muted-foreground">
                        Template layout unavailable; cannot render form view.
                    </p>
                {:else}
                    {#each selectedEvaluationTemplate.sections as section}
                        <div class="rounded-md border p-3 space-y-3">
                            <div>
                                <p class="font-medium">{section.title}</p>
                                {#if section.description}
                                    <p class="text-xs text-muted-foreground">{section.description}</p>
                                {/if}
                            </div>

                            {#each section.questions as question}
                                {@const ans = findAnswer(question.id)}
                                <div class="rounded border p-3 space-y-1">
                                    <div class="text-sm font-medium">{question.text}</div>
                                    {#if question.type === "OPEN_TEXT"}
                                        <p class="text-sm text-muted-foreground">{ans?.textResponse || "No answer submitted."}</p>
                                    {:else if question.type === "SINGLE_CHOICE"}
                                        <p class="text-sm text-muted-foreground">{ans?.selectedOptions?.[0] || ans?.value || "No answer submitted."}</p>
                                    {:else if question.type === "MULTIPLE_CHOICE"}
                                        <p class="text-sm text-muted-foreground">{ans?.selectedOptions?.length ? ans.selectedOptions.join(", ") : "No answer submitted."}</p>
                                    {:else if question.type === "BOOLEAN"}
                                        <p class="text-sm text-muted-foreground">{ans?.value === "true" || ans?.value === true ? "Yes" : ans?.value === "false" || ans?.value === false ? "No" : "No answer submitted."}</p>
                                    {:else if question.type === "NUMERIC_RATING" || question.type === "RATING"}
                                        <p class="text-sm text-muted-foreground">{ans?.value ?? "No answer submitted."}</p>
                                    {:else}
                                        <p class="text-sm text-muted-foreground">{ans?.value ?? ans?.textResponse ?? "No answer submitted."}</p>
                                    {/if}
                                </div>
                            {/each}
                        </div>
                    {/each}
                {/if}
            </div>
        {/if}
    </Dialog.Content>
</Dialog.Root>
