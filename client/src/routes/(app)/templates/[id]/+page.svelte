<script lang="ts">
    import { page } from "$app/stores";
    import { goto } from "$app/navigation";
    import api from "$lib/api.js";
    import { onMount } from "svelte";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Separator } from "$lib/components/ui/separator/index.js";
    import * as AlertDialog from "$lib/components/ui/alert-dialog/index.js";
    import { toast } from "svelte-sonner";
    import {
        ArrowLeft,
        Loader2,
        Edit,
        Trash2,
        Upload,
        AlertTriangle,
        CheckCircle,
    } from "@lucide/svelte";

    let templateId = $state($page.params.id);
    let template = $state<any>(null);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let isProcessing = $state(false);

    async function fetchTemplate() {
        loading = true;
        try {
            const res = await api.get(`/templates/${templateId}`);
            template = res.data;
        } catch (err: any) {
            console.error(err);
            error = "Failed to load template.";
            toast.error("Failed to load template");
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchTemplate();
    });

    async function handlePublish() {
        isProcessing = true;
        try {
            const res = await api.post(`/templates/${templateId}/publish`);
            template = res.data; // Update state
            toast.success("Template published successfully!");
        } catch (err: any) {
            console.error(err);
            toast.error(
                "Failed to publish: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleDeprecate() {
        isProcessing = true;
        try {
            const res = await api.post(`/templates/${templateId}/deprecate`);
            template = res.data;
            toast.success("Template deprecated.");
        } catch (err) {
            console.error(err);
            toast.error("Failed to deprecate template.");
        } finally {
            isProcessing = false;
        }
    }

    async function handleDelete() {
        isProcessing = true;
        try {
            await api.delete(`/templates/${templateId}`);
            toast.success("Template deleted.");
            goto("/templates");
        } catch (err) {
            console.error(err);
            toast.error("Failed to delete template.");
            isProcessing = false;
        }
    }

    function getStatusColor(status: string) {
        switch (status) {
            case "PUBLISHED":
                return "bg-green-500 hover:bg-green-600";
            case "DEPRECATED":
                return "bg-yellow-500 hover:bg-yellow-600";
            default:
                return "bg-blue-500 hover:bg-blue-600"; // Draft
        }
    }
</script>

<div class="flex flex-col gap-6 pb-20">
    <div class="flex items-center gap-4">
        <Button
            variant="ghost"
            size="icon"
            href="/templates"
            onclick={() => goto("/templates")}
        >
            <ArrowLeft class="h-4 w-4" />
        </Button>
        {#if template}
            <div class="flex flex-col">
                <div class="flex items-center gap-3">
                    <h1 class="text-lg font-semibold md:text-2xl">
                        {template.name}
                    </h1>
                    <Badge class={getStatusColor(template.status)}
                        >{template.status}</Badge
                    >
                </div>
                <span class="text-sm text-muted-foreground"
                    >Version {template.currentVersion} • {template.category}</span
                >
            </div>
        {:else if loading}
            <h1 class="text-lg font-semibold md:text-2xl">Loading...</h1>
        {:else}
            <h1 class="text-lg font-semibold md:text-2xl text-red-500">
                Error
            </h1>
        {/if}

        <div class="ml-auto flex items-center gap-2">
            {#if template}
                {#if template.status === "DRAFT"}
                    <Button
                        variant="outline"
                        size="sm"
                        href={`/templates/${templateId}/edit`}
                        onclick={() => goto(`/templates/${templateId}/edit`)}
                    >
                        <Edit class="mr-2 h-4 w-4" /> Edit
                    </Button>

                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button
                                variant="default"
                                size="sm"
                                disabled={isProcessing}
                                class="bg-primary hover:bg-primary/90"
                            >
                                {#if isProcessing}
                                    <Loader2
                                        class="mr-2 h-4 w-4 animate-spin"
                                    />
                                {:else}
                                    <Upload class="mr-2 h-4 w-4" />
                                {/if}
                                Publish
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title
                                    >Are you sure?</AlertDialog.Title
                                >
                                <AlertDialog.Description>
                                    This will publish the template. Once
                                    published, it cannot be edited directly (a
                                    new version will be created).
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action onclick={handlePublish}
                                    >Publish</AlertDialog.Action
                                >
                            </AlertDialog.Footer>
                        </AlertDialog.Content>
                    </AlertDialog.Root>

                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button
                                variant="destructive"
                                size="sm"
                                disabled={isProcessing}
                            >
                                <Trash2 class="mr-2 h-4 w-4" /> Delete
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title
                                    >Are you absolutely sure?</AlertDialog.Title
                                >
                                <AlertDialog.Description>
                                    This action cannot be undone. This will
                                    permanently delete the template.
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action
                                    class="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                    onclick={handleDelete}
                                    >Delete</AlertDialog.Action
                                >
                            </AlertDialog.Footer>
                        </AlertDialog.Content>
                    </AlertDialog.Root>
                {:else if template.status === "PUBLISHED"}
                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button
                                variant="outline"
                                size="sm"
                                disabled={isProcessing}
                            >
                                <AlertTriangle class="mr-2 h-4 w-4" /> Deprecate
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title
                                    >Deprecate Template?</AlertDialog.Title
                                >
                                <AlertDialog.Description>
                                    This will mark the template as deprecated.
                                    New campaigns cannot use it.
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action onclick={handleDeprecate}
                                    >Deprecate</AlertDialog.Action
                                >
                            </AlertDialog.Footer>
                        </AlertDialog.Content>
                    </AlertDialog.Root>
                {/if}
            {/if}
        </div>
    </div>

    {#if loading}
        <div class="flex h-40 items-center justify-center">
            <Loader2 class="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
    {:else if error}
        <div
            class="rounded-md bg-red-50 p-4 text-red-700 dark:bg-red-900/30 dark:text-red-400"
        >
            {error}
        </div>
    {:else if template}
        <div class="grid gap-6 md:grid-cols-3">
            <div class="md:col-span-2 space-y-6">
                <!-- Structure Card (Same as before) -->
                <Card.Root>
                    <Card.Header>
                        <Card.Title>Structure</Card.Title>
                    </Card.Header>
                    <Card.Content class="space-y-6">
                        {#if template.sections && template.sections.length > 0}
                            {#each template.sections as section}
                                <div class="space-y-4">
                                    <div
                                        class="flex items-center justify-between border-b pb-2"
                                    >
                                        <div>
                                            <h3 class="text-lg font-medium">
                                                {section.title}
                                            </h3>
                                            {#if section.description}
                                                <p
                                                    class="text-sm text-muted-foreground"
                                                >
                                                    {section.description}
                                                </p>
                                            {/if}
                                        </div>
                                        <Badge variant="outline"
                                            >Weight: {section.weight}</Badge
                                        >
                                    </div>
                                    <div class="pl-4 space-y-3">
                                        {#each section.questions as question}
                                            <div
                                                class="rounded-lg border p-3 bg-muted/10"
                                            >
                                                <div
                                                    class="flex justify-between items-start gap-4"
                                                >
                                                    <div class="space-y-1">
                                                        <p
                                                            class="font-medium text-sm"
                                                        >
                                                            {question.text}
                                                            {#if question.required}<span
                                                                    class="text-red-500"
                                                                    >*</span
                                                                >{/if}
                                                        </p>
                                                        <div
                                                            class="flex gap-2 text-xs text-muted-foreground"
                                                        >
                                                            <Badge
                                                                variant="secondary"
                                                                class="text-[10px] h-5"
                                                                >{question.type}</Badge
                                                            >
                                                            {#if question.weight > 1}<span
                                                                    >Weight: {question.weight}</span
                                                                >{/if}
                                                        </div>
                                                        {#if question.options && question.options.length > 0}
                                                            <ul
                                                                class="list-disc list-inside text-xs text-muted-foreground mt-2 pl-2"
                                                            >
                                                                {#each question.options as opt}
                                                                    <li>
                                                                        {opt}
                                                                    </li>
                                                                {/each}
                                                            </ul>
                                                        {/if}
                                                    </div>
                                                </div>
                                            </div>
                                        {/each}
                                    </div>
                                </div>
                            {/each}
                        {:else}
                            <div
                                class="text-center py-10 text-muted-foreground"
                            >
                                No sections defined.
                            </div>
                        {/if}
                    </Card.Content>
                </Card.Root>
            </div>

            <div class="space-y-6">
                <!-- Details Card (Same as before) -->
                <Card.Root>
                    <Card.Header>
                        <Card.Title>Details</Card.Title>
                    </Card.Header>
                    <Card.Content class="space-y-4">
                        <div class="grid gap-1">
                            <span
                                class="text-sm font-medium text-muted-foreground"
                                >Description</span
                            >
                            <p class="text-sm">
                                {template.description || "No description"}
                            </p>
                        </div>
                        <Separator />
                        <div class="grid gap-1">
                            <span
                                class="text-sm font-medium text-muted-foreground"
                                >Scoring Method</span
                            >
                            <div class="flex items-center gap-2">
                                <span
                                    class="text-sm font-mono bg-muted px-2 py-1 rounded"
                                    >{template.scoringMethod}</span
                                >
                            </div>
                        </div>
                        <Separator />
                        <div class="grid gap-1">
                            <span
                                class="text-sm font-medium text-muted-foreground"
                                >Created By</span
                            >
                            <p class="text-sm">{template.createdBy}</p>
                        </div>
                        <div class="grid gap-1">
                            <span
                                class="text-sm font-medium text-muted-foreground"
                                >Last Updated</span
                            >
                            <p class="text-sm">
                                {new Date(template.updatedAt).toLocaleString()}
                            </p>
                        </div>
                    </Card.Content>
                </Card.Root>
            </div>
        </div>
    {/if}
</div>
