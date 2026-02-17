<script lang="ts">
    import { auth } from "$lib/stores/auth.svelte.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Activity, Users, FileText, TrendingUp } from "@lucide/svelte";

    // Mock data for dashboard
    const stats = [
        {
            title: "Total Campaigns",
            value: "12",
            icon: Users,
            description: "+2 from last month",
        },
        {
            title: "Active Evaluations",
            value: "24",
            icon: Activity,
            description: "+5 since yesterday",
        },
        {
            title: "Templates",
            value: "4",
            icon: FileText,
            description: "Updated 2 days ago",
        },
        {
            title: "Completion Rate",
            value: "85%",
            icon: TrendingUp,
            description: "+12% from last month",
        },
    ];
</script>

<div class="flex flex-col gap-4">
    <div class="flex items-center">
        <h1 class="text-lg font-semibold md:text-2xl">Dashboard</h1>
    </div>

    <div class="grid gap-4 md:grid-cols-2 md:gap-8 lg:grid-cols-4">
        {#each stats as stat}
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
