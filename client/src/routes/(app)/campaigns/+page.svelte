<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Loader2, Plus, MoreHorizontal } from "@lucide/svelte";
    import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
    import { goto } from "$app/navigation";
    import { toast } from "svelte-sonner";

    let campaigns = $state<any[]>([]);
    let isLoading = $state(true);
    let selectedCampaignId = $state("");
    let reconcileReport = $state<any>(null);
    let reconcileResult = $state<any>(null);
    let backfillResult = $state<any>(null);
    let dryRunBackfill = $state(true);
    let maxCampaigns = $state(1000);
    let isReconciling = $state(false);

    async function fetchCampaigns() {
        isLoading = true;
        try {
            const response = await api.get("/campaigns");
            // The API returns a list of campaigns directly?
            // Controller: return ResponseEntity.ok(campaigns); -> List<CampaignResponse>
            campaigns = response.data;
            if (!selectedCampaignId && response.data.length > 0) {
                selectedCampaignId = response.data[0].id;
            }
        } catch (error) {
            console.error("Failed to fetch campaigns:", error);
            toast.error("Failed to fetch campaigns");
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        fetchCampaigns();
    });

    function formatDate(dateString: string) {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString();
    }

    function getStatusVariant(status: string) {
        switch (status) {
            case "ACTIVE":
                return "default"; // primary
            case "DRAFT":
                return "secondary";
            case "CLOSED":
                return "destructive"; // maybe not destructive, but distinctive
            case "ARCHIVED":
                return "outline";
            default:
                return "outline";
        }
    }

    async function handleAction(
        id: string,
        action: "activate" | "close" | "archive",
    ) {
        try {
            await api.post(`/campaigns/${id}/${action}`);
            fetchCampaigns(); // Refresh list
        } catch (error) {
            console.error(`Failed to ${action} campaign:`, error);
            toast.error(`Failed to ${action} campaign`);
        }
    }

    async function fetchReconcileForCampaign() {
        if (!selectedCampaignId) return;
        isReconciling = true;
        try {
            const res = await api.get(`/campaigns/${selectedCampaignId}/assignments/reconcile`);
            reconcileResult = res.data;
        } catch (error) {
            console.error("Failed to reconcile campaign:", error);
            toast.error("Failed to run campaign reconciliation");
        } finally {
            isReconciling = false;
        }
    }

    async function fetchReconcileReport() {
        isReconciling = true;
        try {
            const res = await api.get("/campaigns/assignments/reconcile/report", {
                params: { maxCampaigns },
            });
            reconcileReport = res.data;
        } catch (error) {
            console.error("Failed to get reconciliation report:", error);
            toast.error("Failed to load reconciliation report");
        } finally {
            isReconciling = false;
        }
    }

    async function runBackfill() {
        isReconciling = true;
        try {
            const res = await api.post("/campaigns/assignments/backfill", null, {
                params: { dryRun: dryRunBackfill, maxCampaigns },
            });
            backfillResult = res.data;
            toast.success(dryRunBackfill ? "Backfill dry-run completed" : "Backfill completed");
        } catch (error) {
            console.error("Failed to run backfill:", error);
            toast.error("Failed to run assignment backfill");
        } finally {
            isReconciling = false;
        }
    }
</script>

<div class="flex items-center justify-between">
    <h1 class="text-lg font-semibold md:text-2xl">Campaigns</h1>
    <Button href="/campaigns/new" onclick={() => goto("/campaigns/new")}>
        <Plus class="mr-2 h-4 w-4" />
        New Campaign
    </Button>
</div>

<Card.Root>
    <Card.Header>
        <Card.Title>Assignment Storage Tools</Card.Title>
        <Card.Description>
            Reconciliation, parity report, and JSON-to-relational backfill.
        </Card.Description>
    </Card.Header>
    <Card.Content class="grid gap-3 md:grid-cols-3">
        <div class="grid gap-2">
            <Label>Campaign</Label>
            <Input bind:value={selectedCampaignId} placeholder="campaign id" />
        </div>
        <div class="grid gap-2">
            <Label>Max Campaigns</Label>
            <Input type="number" min="1" bind:value={maxCampaigns} />
        </div>
        <div class="flex items-center gap-2 self-end">
            <Checkbox checked={dryRunBackfill} onCheckedChange={(v) => (dryRunBackfill = v)} />
            <Label>Backfill Dry Run</Label>
        </div>
        <div class="flex flex-wrap gap-2 md:col-span-3">
            <Button variant="outline" onclick={fetchReconcileForCampaign} disabled={isReconciling || !selectedCampaignId}>
                Reconcile Campaign
            </Button>
            <Button variant="outline" onclick={fetchReconcileReport} disabled={isReconciling}>
                Reconcile Report
            </Button>
            <Button onclick={runBackfill} disabled={isReconciling}>
                {#if isReconciling}
                    <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                {/if}
                Run Backfill
            </Button>
        </div>
        {#if reconcileResult}
            <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto md:col-span-3">{JSON.stringify(reconcileResult, null, 2)}</pre>
        {/if}
        {#if reconcileReport}
            <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto md:col-span-3">{JSON.stringify(reconcileReport, null, 2)}</pre>
        {/if}
        {#if backfillResult}
            <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto md:col-span-3">{JSON.stringify(backfillResult, null, 2)}</pre>
        {/if}
    </Card.Content>
</Card.Root>

<div class="rounded-md border">
    <Table.Root>
        <Table.Header>
            <Table.Row>
                <Table.Head>Name</Table.Head>
                <Table.Head>Status</Table.Head>
                <Table.Head>Start Date</Table.Head>
                <Table.Head>End Date</Table.Head>
                <Table.Head class="text-right">Actions</Table.Head>
            </Table.Row>
        </Table.Header>
        <Table.Body>
            {#if isLoading}
                <Table.Row>
                    <Table.Cell colspan={5} class="h-24 text-center">
                        <Loader2 class="mx-auto h-6 w-6 animate-spin" />
                    </Table.Cell>
                </Table.Row>
            {:else if campaigns.length === 0}
                <Table.Row>
                    <Table.Cell colspan={5} class="h-24 text-center">
                        No campaigns found.
                    </Table.Cell>
                </Table.Row>
            {:else}
                {#each campaigns as campaign (campaign.id)}
                    <Table.Row>
                        <Table.Cell class="font-medium"
                            >{campaign.name}</Table.Cell
                        >
                        <Table.Cell>
                            <Badge variant={getStatusVariant(campaign.status)}
                                >{campaign.status}</Badge
                            >
                        </Table.Cell>
                        <Table.Cell>{formatDate(campaign.startDate)}</Table.Cell
                        >
                        <Table.Cell>{formatDate(campaign.endDate)}</Table.Cell>
                        <Table.Cell class="text-right">
                            <DropdownMenu.Root>
                                <DropdownMenu.Trigger>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        class="h-8 w-8"
                                    >
                                        <MoreHorizontal class="h-4 w-4" />
                                        <span class="sr-only">Toggle menu</span>
                                    </Button>
                                </DropdownMenu.Trigger>
                                <DropdownMenu.Content align="end">
                                    <DropdownMenu.Label
                                        >Actions</DropdownMenu.Label
                                    >
                                    <DropdownMenu.Item
                                        onSelect={() =>
                                            goto(`/campaigns/${campaign.id}`)}
                                        >View Details</DropdownMenu.Item
                                    >
                                    {#if campaign.status === "DRAFT"}
                                        <DropdownMenu.Item
                                            onSelect={() =>
                                                handleAction(
                                                    campaign.id,
                                                    "activate",
                                                )}>Activate</DropdownMenu.Item
                                        >
                                    {/if}
                                    {#if campaign.status === "ACTIVE"}
                                        <DropdownMenu.Item
                                            onSelect={() =>
                                                handleAction(
                                                    campaign.id,
                                                    "close",
                                                )}>Close</DropdownMenu.Item
                                        >
                                    {/if}
                                    {#if campaign.status === "CLOSED"}
                                        <DropdownMenu.Item
                                            onSelect={() =>
                                                handleAction(
                                                    campaign.id,
                                                    "archive",
                                                )}>Archive</DropdownMenu.Item
                                        >
                                    {/if}
                                </DropdownMenu.Content>
                            </DropdownMenu.Root>
                        </Table.Cell>
                    </Table.Row>
                {/each}
            {/if}
        </Table.Body>
    </Table.Root>
</div>
