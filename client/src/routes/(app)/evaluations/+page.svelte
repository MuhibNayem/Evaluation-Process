<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Loader2, Calendar, User, ArrowRight } from "@lucide/svelte";
    import { goto } from "$app/navigation";

    let assignments = $state<any[]>([]);
    let isLoading = $state(true);

    async function fetchAssignments() {
        isLoading = true;
        try {
            const response = await api.get("/campaigns/assignments/me");
            assignments = response.data;
        } catch (error) {
            console.error("Failed to fetch assignments:", error);
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        fetchAssignments();
    });

    function formatDate(dateString: string) {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString();
    }

    function handleStart(assignment: any) {
        if (assignment.evaluationId) {
            goto(`/evaluations/${assignment.evaluationId}`);
        } else {
            goto(`/evaluations/new/${assignment.id}`);
        }
    }
</script>

<div class="flex items-center justify-between mb-6">
    <h1 class="text-lg font-semibold md:text-2xl">My Evaluations</h1>
</div>

{#if isLoading}
    <div class="flex justify-center p-8">
        <Loader2 class="h-8 w-8 animate-spin text-primary" />
    </div>
{:else if assignments.length === 0}
    <div class="text-center p-8 border rounded-lg bg-muted/10">
        <p class="text-muted-foreground">You have no pending evaluations.</p>
    </div>
{:else}
    <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {#each assignments as assignment}
            <Card.Root>
                <Card.Header>
                    <div class="flex justify-between items-start">
                        <Badge
                            variant={assignment.status === "COMPLETED"
                                ? "default"
                                : "outline"}
                        >
                            {assignment.status}
                        </Badge>
                        {#if assignment.endDate}
                            <span
                                class="text-xs text-muted-foreground flex items-center gap-1"
                            >
                                <Calendar class="h-3 w-3" /> due {formatDate(
                                    assignment.endDate,
                                )}
                            </span>
                        {/if}
                    </div>
                    <Card.Title class="mt-2 text-lg"
                        >{assignment.campaignName}</Card.Title
                    >
                    <Card.Description class="flex items-center gap-2 mt-1">
                        <User class="h-4 w-4" /> Evaluatee: {assignment.evaluateeId}
                    </Card.Description>
                </Card.Header>
                <Card.Content></Card.Content>
                <Card.Footer>
                    <Button
                        class="w-full"
                        onclick={() => handleStart(assignment)}
                        variant={assignment.status === "COMPLETED"
                            ? "secondary"
                            : "default"}
                    >
                        {#if assignment.status === "COMPLETED"}
                            View Submission
                        {:else}
                            {assignment.evaluationId
                                ? "Continue Evaluation"
                                : "Start Evaluation"}
                            <ArrowRight class="ml-2 h-4 w-4" />
                        {/if}
                    </Button>
                </Card.Footer>
            </Card.Root>
        {/each}
    </div>
{/if}
