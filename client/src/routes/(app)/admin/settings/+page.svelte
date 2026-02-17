<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Separator } from "$lib/components/ui/separator/index.js";
    import { Loader2, Save, RotateCcw } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let settings = $state<any[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let saving = $state<string | null>(null); // Key of setting being saved

    // Group settings by category
    let groupedSettings = $derived.by(() => {
        const groups: Record<string, any[]> = {};
        settings.forEach((s) => {
            if (!groups[s.category]) {
                groups[s.category] = [];
            }
            groups[s.category].push(s);
        });
        return groups;
    });

    async function fetchSettings() {
        loading = true;
        try {
            const res = await api.get("/admin/settings");
            settings = res.data.sort((a: any, b: any) =>
                a.key.localeCompare(b.key),
            );
        } catch (err) {
            console.error(err);
            error = "Failed to load settings.";
            toast.error("Failed to load settings");
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchSettings();
    });

    async function handleSave(key: string, value: string) {
        saving = key;
        try {
            const res = await api.put(`/admin/settings/${key}`, { value });
            // Update local state
            settings = settings.map((s) =>
                s.key === key ? { ...s, value: res.data.value } : s,
            );
            toast.success("Setting updated successfully");
        } catch (err: any) {
            console.error(err);
            toast.error("Failed to update setting");
        } finally {
            saving = null;
        }
    }
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">System Settings</h1>
        <p class="text-muted-foreground">
            Manage global configuration for the evaluation system.
        </p>
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
    {:else}
        <div class="grid gap-6">
            {#each Object.entries(groupedSettings) as [category, items]}
                <Card.Root>
                    <Card.Header>
                        <Card.Title>{category}</Card.Title>
                    </Card.Header>
                    <Card.Content class="space-y-6">
                        {#each items as setting (setting.key)}
                            <div class="grid gap-2">
                                <div class="flex items-center justify-between">
                                    <label
                                        for={setting.key}
                                        class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                                    >
                                        {setting.key}
                                    </label>
                                    <Badge variant="outline" class="text-xs"
                                        >Default</Badge
                                    >
                                </div>
                                <p class="text-xs text-muted-foreground">
                                    {setting.description}
                                </p>
                                <div class="flex gap-2">
                                    <Input
                                        id={setting.key}
                                        bind:value={setting.value}
                                        disabled={saving === setting.key}
                                    />
                                    <Button
                                        size="icon"
                                        disabled={saving === setting.key}
                                        onclick={() =>
                                            handleSave(
                                                setting.key,
                                                setting.value,
                                            )}
                                    >
                                        {#if saving === setting.key}
                                            <Loader2
                                                class="h-4 w-4 animate-spin"
                                            />
                                        {:else}
                                            <Save class="h-4 w-4" />
                                        {/if}
                                    </Button>
                                </div>
                            </div>
                            <Separator />
                        {/each}
                    </Card.Content>
                </Card.Root>
            {/each}
        </div>
    {/if}
</div>
