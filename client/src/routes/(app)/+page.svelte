<script lang="ts">
    import { auth } from "$lib/stores/auth.svelte.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import {
        Activity,
        Users,
        FileText,
        TrendingUp,
        Loader2,
    } from "@lucide/svelte";
    import api from "$lib/api.js";
    import { onMount } from "svelte";

    let stats = $state({
        totalCampaigns: 0,
        activeCampaigns: 0,
        totalTemplates: 0,
        activeEvaluations: 0,
        completedEvaluations: 0,
        completionRate: 0,
        recentActivity: [],
    });
    let loading = $state(true);

    async function fetchStats() {
        loading = true;
        try {
            const res = await api.get("/dashboard/stats");
            stats = res.data;
        } catch (err) {
            console.error("Failed to load dashboard stats", err);
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchStats();
    });

    let statCards = $derived([
        {
            title: "Total Campaigns",
            value: stats.totalCampaigns.toString(),
            icon: Users,
            description: `${stats.activeCampaigns} Active`,
        },
        {
            title: "Pending Evaluations",
            value: stats.activeEvaluations.toString(),
            icon: Activity,
            description: `${stats.completedEvaluations} Completed`,
        },
        {
            title: "Templates",
            value: stats.totalTemplates.toString(),
            icon: FileText,
            description: "Available templates",
        },
        {
            title: "Completion Rate",
            value: `${stats.completionRate.toFixed(1)}%`,
            icon: TrendingUp,
            description: "Overall progress",
        },
    ]);
</script>

<div class="flex flex-col gap-4">
    <div class="flex items-center">
        <h1 class="text-lg font-semibold md:text-2xl">Dashboard</h1>
    </div>

    {#if loading}
        <div class="flex h-40 items-center justify-center">
            <Loader2 class="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
    {:else}
        <div class="grid gap-4 md:grid-cols-2 md:gap-8 lg:grid-cols-4">
            {#each statCards as stat}
                <Card.Root>
                    <Card.Header
                        class="flex flex-row items-center justify-between space-y-0 pb-2"
                    >
                        <Card.Title class="text-sm font-medium">
                            {stat.title}
                        </Card.Title>
                        <stat.icon class="h-4 w-4 text-muted-foreground" />
                    </Card.Header>
                    <Card.Content>
                        <div class="text-2xl font-bold">{stat.value}</div>
                        <p class="text-xs text-muted-foreground">
                            {stat.description}
                        </p>
                    </Card.Content>
                </Card.Root>
            {/each}
        </div>
    {/if}

    <div class="grid gap-4 md:gap-8 lg:grid-cols-2 xl:grid-cols-3">
        <Card.Root class="xl:col-span-2">
            <Card.Header>
                <Card.Title>Recent Activity</Card.Title>
                <Card.Description>
                    Recent evaluations and campaign updates.
                </Card.Description>
            </Card.Header>
            <Card.Content>
                <div class="space-y-4">
                    <!-- Placeholder for activity list -->
                    <p class="text-sm text-muted-foreground">
                        No recent activity to show.
                    </p>
                </div>
            </Card.Content>
        </Card.Root>
        <Card.Root>
            <Card.Header>
                <Card.Title>Quick Actions</Card.Title>
            </Card.Header>
            <Card.Content class="grid gap-2">
                <!-- Placeholder for quick actions -->
                <p class="text-sm text-muted-foreground">Create Campaign</p>
                <p class="text-sm text-muted-foreground">New Template</p>
            </Card.Content>
        </Card.Root>
    </div>
</div>
