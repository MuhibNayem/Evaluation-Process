<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Loader2, Plus, MoreHorizontal } from "@lucide/svelte";
    import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
    import { goto } from "$app/navigation";

    let templates = $state<any[]>([]);
    let isLoading = $state(true);

    async function fetchTemplates() {
        isLoading = true;
        try {
            const response = await api.get("/templates");
            templates = response.data;
        } catch (error) {
            console.error("Failed to fetch templates:", error);
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        fetchTemplates();
    });

    function formatDate(dateString: string) {
        if (!dateString) return "N/A";
        return new Date(dateString).toLocaleDateString();
    }
</script>

<div class="flex items-center justify-between">
    <h1 class="text-lg font-semibold md:text-2xl">Templates</h1>
    <Button href="/templates/new" onclick={() => goto("/templates/new")}>
        <Plus class="mr-2 h-4 w-4" />
        New Template
    </Button>
</div>

<div class="rounded-md border">
    <Table.Root>
        <Table.Header>
            <Table.Row>
                <Table.Head>Name</Table.Head>
                <Table.Head>Category</Table.Head>
                <Table.Head>Version</Table.Head>
                <Table.Head>Status</Table.Head>
                <Table.Head>Created At</Table.Head>
                <Table.Head class="text-right">Actions</Table.Head>
            </Table.Row>
        </Table.Header>
        <Table.Body>
            {#if isLoading}
                <Table.Row>
                    <Table.Cell colspan={6} class="h-24 text-center">
                        <Loader2 class="mx-auto h-6 w-6 animate-spin" />
                    </Table.Cell>
                </Table.Row>
            {:else if templates.length === 0}
                <Table.Row>
                    <Table.Cell colspan={6} class="h-24 text-center">
                        No templates found.
                    </Table.Cell>
                </Table.Row>
            {:else}
                {#each templates as template (template.id)}
                    <Table.Row>
                        <Table.Cell class="font-medium"
                            >{template.name}</Table.Cell
                        >
                        <Table.Cell>
                            <Badge variant="outline">{template.category}</Badge>
                        </Table.Cell>
                        <Table.Cell>v{template.currentVersion}</Table.Cell>
                        <Table.Cell>
                            <Badge
                                variant={template.status === "PUBLISHED"
                                    ? "default"
                                    : "secondary"}>{template.status}</Badge
                            >
                        </Table.Cell>
                        <Table.Cell>{formatDate(template.createdAt)}</Table.Cell
                        >
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
                                            goto(`/templates/${template.id}`)}
                                        >View Details</DropdownMenu.Item
                                    >
                                    <DropdownMenu.Item
                                        onSelect={() =>
                                            goto(
                                                `/templates/${template.id}/edit`,
                                            )}>Edit</DropdownMenu.Item
                                    >
                                </DropdownMenu.Content>
                            </DropdownMenu.Root>
                        </Table.Cell>
                    </Table.Row>
                {/each}
            {/if}
        </Table.Body>
    </Table.Root>
</div>
