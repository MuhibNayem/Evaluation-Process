<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Switch } from "$lib/components/ui/switch/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import { Separator } from "$lib/components/ui/separator/index.js";
    import { Loader2, Save, CircleHelp } from "@lucide/svelte";
    import DataView from "$lib/components/data-view.svelte";
    import { toast } from "svelte-sonner";

    let settings = $state<any[]>([]);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let saving = $state<string | null>(null); // Key of setting being saved
    let categoryFilter = $state("");
    let keyLookup = $state("");
    let selectedSetting = $state<any | null>(null);
    let infoOpenKey = $state<string | null>(null);
    let campaignId = $state("");
    let campaigns = $state<any[]>([]);
    let campaignOverrideKey = $state("");
    let campaignOverrideValue = $state("");
    let campaignOverrides = $state<any[]>([]);
    let campaignOverridesLoading = $state(false);

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

    async function fetchCampaigns() {
        try {
            const res = await api.get("/campaigns");
            campaigns = res.data;
            if (!campaignId && campaigns.length > 0) {
                campaignId = campaigns[0].id;
            }
        } catch (err) {
            console.error(err);
            toast.error("Failed to load campaigns");
        }
    }

    onMount(() => {
        fetchSettings();
        fetchCampaigns();
    });

    function campaignLabel(campaign: any) {
        return `${campaign.name} [${campaign.status}]`;
    }

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

    function isBooleanValue(value: unknown) {
        const v = String(value ?? "").trim().toLowerCase();
        return v === "true" || v === "false";
    }

    function isNumericValue(value: unknown) {
        const v = String(value ?? "").trim();
        if (!v || isBooleanValue(v)) return false;
        return /^-?\d+(\.\d+)?$/.test(v);
    }

    function inputTypeLabel(setting: any) {
        if (isBooleanValue(setting.value)) return "BOOLEAN";
        if (isNumericValue(setting.value)) return "NUMBER";
        return "TEXT";
    }

    function toggleSettingInfo(key: string) {
        infoOpenKey = infoOpenKey === key ? null : key;
    }

    async function fetchSettingsByCategory() {
        if (!categoryFilter.trim()) return;
        loading = true;
        selectedSetting = null;
        try {
            const res = await api.get(`/admin/settings/category/${categoryFilter.trim()}`);
            settings = res.data.sort((a: any, b: any) => a.key.localeCompare(b.key));
            toast.success(`Loaded ${settings.length} setting(s) for ${categoryFilter.toUpperCase()}`);
        } catch (err: any) {
            console.error(err);
            toast.error(err.response?.data?.detail || "Failed to load settings by category");
        } finally {
            loading = false;
        }
    }

    async function fetchSettingByKey() {
        if (!keyLookup.trim()) return;
        loading = true;
        try {
            const res = await api.get(`/admin/settings/${keyLookup.trim()}`);
            selectedSetting = res.data;
            settings = [res.data];
            toast.success(`Loaded setting: ${res.data.key}`);
        } catch (err: any) {
            console.error(err);
            toast.error(err.response?.data?.detail || "Failed to load setting by key");
        } finally {
            loading = false;
        }
    }

    async function loadCampaignOverrides() {
        if (!campaignId.trim()) return;
        campaignOverridesLoading = true;
        try {
            const res = await api.get(`/admin/settings/campaigns/${campaignId}`);
            campaignOverrides = res.data;
        } catch (err: any) {
            console.error(err);
            toast.error(err.response?.data?.detail || "Failed to load campaign overrides");
        } finally {
            campaignOverridesLoading = false;
        }
    }

    async function saveCampaignOverride() {
        if (!campaignId.trim() || !campaignOverrideKey.trim()) return;
        try {
            await api.put(
                `/admin/settings/campaigns/${campaignId}/${campaignOverrideKey}`,
                { value: campaignOverrideValue },
            );
            toast.success("Campaign override saved");
            await loadCampaignOverrides();
        } catch (err: any) {
            console.error(err);
            toast.error(err.response?.data?.detail || "Failed to save campaign override");
        }
    }

    async function deleteCampaignOverride(key: string) {
        if (!campaignId.trim()) return;
        try {
            await api.delete(`/admin/settings/campaigns/${campaignId}/${key}`);
            toast.success("Campaign override deleted");
            await loadCampaignOverrides();
        } catch (err: any) {
            console.error(err);
            toast.error(err.response?.data?.detail || "Failed to delete campaign override");
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
            <Card.Root>
                <Card.Header>
                    <Card.Title>Setting Lookup</Card.Title>
                </Card.Header>
                <Card.Content class="space-y-3">
                    <div class="grid gap-2 md:grid-cols-2">
                        <div class="space-y-2">
                            <Input bind:value={categoryFilter} placeholder="SCORING | CAMPAIGN | NOTIFICATION | FEATURES | PAGINATION" />
                            <div class="flex gap-2">
                                <Button variant="outline" onclick={fetchSettingsByCategory} disabled={!categoryFilter.trim()}>
                                    Load by Category
                                </Button>
                                <Button variant="ghost" onclick={fetchSettings}>Load All</Button>
                            </div>
                        </div>
                        <div class="space-y-2">
                            <Input bind:value={keyLookup} placeholder="setting key (e.g. scoring.default-method)" />
                            <div class="flex gap-2">
                                <Button variant="outline" onclick={fetchSettingByKey} disabled={!keyLookup.trim()}>
                                    Load by Key
                                </Button>
                                <Button variant="ghost" onclick={() => (selectedSetting = null)}>Clear</Button>
                            </div>
                        </div>
                    </div>
                    {#if selectedSetting}
                        <div class="rounded-md border bg-muted/30 p-3">
                            <DataView data={selectedSetting} />
                        </div>
                    {/if}
                </Card.Content>
            </Card.Root>

            <Card.Root>
                <Card.Header>
                    <Card.Title>Campaign Overrides</Card.Title>
                </Card.Header>
                <Card.Content class="space-y-3">
                    <div class="grid gap-2 md:grid-cols-3">
                        <select
                            bind:value={campaignId}
                            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        >
                            <option value="">Select campaign</option>
                            {#each campaigns as campaign}
                                <option value={campaign.id}>{campaignLabel(campaign)}</option>
                            {/each}
                        </select>
                        <Input bind:value={campaignOverrideKey} placeholder="setting key" />
                        <Input bind:value={campaignOverrideValue} placeholder="override value" />
                    </div>
                    <div class="flex gap-2">
                        <Button variant="outline" onclick={loadCampaignOverrides} disabled={campaignOverridesLoading || !campaignId.trim()}>
                            {#if campaignOverridesLoading}
                                <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                            {/if}
                            Load Overrides
                        </Button>
                        <Button onclick={saveCampaignOverride} disabled={!campaignId.trim() || !campaignOverrideKey.trim()}>
                            Save Override
                        </Button>
                    </div>
                    {#if campaignOverrides.length > 0}
                        <div class="space-y-2">
                            {#each campaignOverrides as item}
                                <div class="flex items-center justify-between rounded border p-2 text-sm">
                                    <span><strong>{item.key}</strong> = {item.value}</span>
                                    <Button size="sm" variant="destructive" onclick={() => deleteCampaignOverride(item.key)}>Delete</Button>
                                </div>
                            {/each}
                        </div>
                    {/if}
                </Card.Content>
            </Card.Root>

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
                                    <div class="flex items-center gap-2">
                                        <Badge variant="outline" class="text-xs"
                                            >{inputTypeLabel(setting)}</Badge
                                        >
                                        <Button
                                            type="button"
                                            variant="ghost"
                                            size="icon"
                                            class="h-7 w-7"
                                            onclick={() => toggleSettingInfo(setting.key)}
                                        >
                                            <CircleHelp class="h-4 w-4" />
                                        </Button>
                                    </div>
                                </div>
                                {#if infoOpenKey === setting.key}
                                    <div class="rounded-md border bg-muted/30 p-3 text-xs space-y-1">
                                        <p><strong>What it controls:</strong> {setting.description || "No description provided."}</p>
                                        <p><strong>Category:</strong> {setting.category}</p>
                                        <p><strong>Key:</strong> {setting.key}</p>
                                        <p><strong>Current type:</strong> {inputTypeLabel(setting)}</p>
                                    </div>
                                {/if}
                                <div class="flex gap-2">
                                    {#if isBooleanValue(setting.value)}
                                        <div class="flex flex-1 items-center justify-between rounded-md border px-3 py-2">
                                            <span class="text-sm">{setting.value === "true" ? "Enabled" : "Disabled"}</span>
                                            <Switch
                                                checked={setting.value === "true"}
                                                disabled={saving === setting.key}
                                                onCheckedChange={(v) => (setting.value = v ? "true" : "false")}
                                            />
                                        </div>
                                    {:else if isNumericValue(setting.value)}
                                        <Input
                                            id={setting.key}
                                            type="number"
                                            bind:value={setting.value}
                                            disabled={saving === setting.key}
                                        />
                                    {:else}
                                        <Input
                                            id={setting.key}
                                            bind:value={setting.value}
                                            disabled={saving === setting.key}
                                        />
                                    {/if}
                                    <Button
                                        type="button"
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
