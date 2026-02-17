<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Loader2, Plus, MoreHorizontal } from "@lucide/svelte";
    import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
    import { goto } from "$app/navigation";

    let campaigns = $state<any[]>([]);
    let isLoading = $state(true);

    async function fetchCampaigns() {
        isLoading = true;
        try {
            const response = await api.get("/campaigns");
            // The API returns a list of campaigns directly?
            // Controller: return ResponseEntity.ok(campaigns); -> List<CampaignResponse>
            campaigns = response.data;
        } catch (error) {
            console.error("Failed to fetch campaigns:", error);
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
