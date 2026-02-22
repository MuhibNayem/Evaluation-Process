<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import DataView from "$lib/components/data-view.svelte";
    import { Loader2, Plus, RefreshCcw } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    const QUESTION_TYPES = [
        "SINGLE_CHOICE",
        "MULTIPLE_CHOICE",
        "LIKERT_SCALE",
        "OPEN_TEXT",
        "NUMERIC_RATING",
        "BOOLEAN",
        "MATRIX",
        "RANKING",
        "NPS",
        "FILE_UPLOAD",
    ];

    const SET_ITEM_STATUSES = ["ACTIVE", "INACTIVE"];
    const VERSION_STATUSES = ["DRAFT", "ACTIVE", "RETIRED"];

    let loadingSets = $state(false);
    let loadingItems = $state(false);
    let loadingVersions = $state(false);
    let loadingCompare = $state(false);

    let sets = $state<any[]>([]);
    let items = $state<any[]>([]);
    let versions = $state<any[]>([]);

    let selectedSetId = $state("");
    let selectedItemId = $state("");

    let compareResponse = $state<any>(null);

    let filters = $state({
        tenantId: "tenant-001",
        setStatus: "",
        itemStatus: "",
        versionStatus: "",
    });

    let setForm = $state({
        tenantId: "tenant-001",
        name: "",
        versionTag: "",
        owner: "admin",
    });

    let itemForm = $state({
        stableKey: "",
        contextType: "",
        categoryName: "",
        defaultType: "NUMERIC_RATING",
        defaultMarks: "10",
    });

    let versionForm = $state({
        status: "DRAFT",
        changeSummary: "",
        questionText: "",
        questionType: "NUMERIC_RATING",
        marks: "10",
        remarksMandatory: false,
    });

    let metadataRows = $state([{ key: "", value: "" }]);
    let compareForm = $state({ fromVersion: 1, toVersion: 1 });

    const setCount = $derived(sets.length);
    const itemCount = $derived(items.length);
    const versionCount = $derived(versions.length);

    function clearCompare() {
        compareResponse = null;
    }

    function statusBadgeVariant(status: string) {
        const s = (status || "").toUpperCase();
        if (s === "ACTIVE") return "default";
        if (s === "INACTIVE" || s === "RETIRED") return "destructive";
        return "secondary";
    }

    function metadataMapFromRows() {
        const out: Record<string, unknown> = {};
        for (const row of metadataRows) {
            const key = (row.key || "").trim();
            if (!key) continue;
            const raw = (row.value || "").trim();
            if (!raw) {
                out[key] = "";
                continue;
            }
            try {
                out[key] = JSON.parse(raw);
            } catch {
                out[key] = raw;
            }
        }
        return out;
    }

    function addMetadataRow() {
        metadataRows = [...metadataRows, { key: "", value: "" }];
    }

    function removeMetadataRow(index: number) {
        metadataRows = metadataRows.filter((_, i) => i !== index);
        if (metadataRows.length === 0) metadataRows = [{ key: "", value: "" }];
    }

    function resetVersionForm() {
        versionForm = {
            status: "DRAFT",
            changeSummary: "",
            questionText: "",
            questionType: "NUMERIC_RATING",
            marks: "10",
            remarksMandatory: false,
        };
        metadataRows = [{ key: "", value: "" }];
    }

    async function loadSets() {
        loadingSets = true;
        try {
            const res = await api.get("/questions-bank/sets", {
                params: {
                    tenantId: filters.tenantId.trim() || undefined,
                    status: filters.setStatus || undefined,
                },
            });
            sets = Array.isArray(res.data) ? res.data : [];

            if (!selectedSetId && sets.length > 0) {
                selectedSetId = String(sets[0].id);
            }

            if (selectedSetId && !sets.some((s) => String(s.id) === String(selectedSetId))) {
                selectedSetId = sets.length > 0 ? String(sets[0].id) : "";
            }

            if (selectedSetId) {
                await loadItems();
            } else {
                items = [];
                versions = [];
                selectedItemId = "";
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load sets");
        } finally {
            loadingSets = false;
        }
    }

    async function createSet() {
        try {
            await api.post("/questions-bank/sets", {
                tenantId: setForm.tenantId || null,
                name: setForm.name,
                versionTag: setForm.versionTag || null,
                owner: setForm.owner || null,
            });
            toast.success("Set created");
            setForm.name = "";
            setForm.versionTag = "";
            await loadSets();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to create set");
        }
    }

    async function loadItems() {
        if (!selectedSetId) {
            items = [];
            versions = [];
            selectedItemId = "";
            return;
        }

        loadingItems = true;
        clearCompare();
        try {
            const res = await api.get(`/questions-bank/sets/${selectedSetId}/items`, {
                params: { status: filters.itemStatus || undefined },
            });
            items = Array.isArray(res.data) ? res.data : [];

            if (!selectedItemId && items.length > 0) {
                selectedItemId = String(items[0].id);
            }

            if (selectedItemId && !items.some((i) => String(i.id) === String(selectedItemId))) {
                selectedItemId = items.length > 0 ? String(items[0].id) : "";
            }

            if (selectedItemId) {
                await loadVersions();
            } else {
                versions = [];
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load items");
        } finally {
            loadingItems = false;
        }
    }

    async function createItem() {
        if (!selectedSetId) {
            toast.error("Select a set first");
            return;
        }
        try {
            await api.post(`/questions-bank/sets/${selectedSetId}/items`, {
                stableKey: itemForm.stableKey,
                contextType: itemForm.contextType || null,
                categoryName: itemForm.categoryName || null,
                defaultType: itemForm.defaultType,
                defaultMarks: Number(itemForm.defaultMarks),
            });
            toast.success("Item created");
            itemForm.stableKey = "";
            await loadItems();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to create item");
        }
    }

    async function loadVersions() {
        if (!selectedItemId) {
            versions = [];
            return;
        }

        loadingVersions = true;
        clearCompare();
        try {
            const res = await api.get(`/questions-bank/items/${selectedItemId}/versions`, {
                params: { status: filters.versionStatus || undefined },
            });
            versions = Array.isArray(res.data) ? res.data : [];

            if (versions.length > 0) {
                const newest = versions[0].versionNo;
                const oldest = versions[versions.length - 1].versionNo;
                compareForm.toVersion = newest;
                compareForm.fromVersion = versions.length > 1 ? versions[1].versionNo : oldest;
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load versions");
        } finally {
            loadingVersions = false;
        }
    }

    async function createVersion() {
        if (!selectedItemId) {
            toast.error("Select an item first");
            return;
        }
        try {
            await api.post(`/questions-bank/items/${selectedItemId}/versions`, {
                status: versionForm.status,
                changeSummary: versionForm.changeSummary || null,
                questionText: versionForm.questionText,
                questionType: versionForm.questionType,
                marks: Number(versionForm.marks),
                remarksMandatory: versionForm.remarksMandatory,
                metadata: metadataMapFromRows(),
            });
            toast.success("Version created");
            await loadVersions();
            await loadItems();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to create version");
        }
    }

    async function activateVersion(versionNo: number) {
        if (!selectedItemId) return;
        try {
            await api.post(`/questions-bank/items/${selectedItemId}/versions/${versionNo}/activate`);
            toast.success(`Version ${versionNo} activated`);
            await loadVersions();
            await loadItems();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to activate version");
        }
    }

    async function compareVersions() {
        if (!selectedItemId) {
            toast.error("Select an item first");
            return;
        }
        loadingCompare = true;
        try {
            const res = await api.get(`/questions-bank/items/${selectedItemId}/versions/compare`, {
                params: {
                    fromVersion: compareForm.fromVersion,
                    toVersion: compareForm.toVersion,
                },
            });
            compareResponse = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to compare versions");
        } finally {
            loadingCompare = false;
        }
    }

    function selectSet(id: string | number) {
        selectedSetId = String(id);
        selectedItemId = "";
        loadItems();
    }

    function selectItem(id: string | number) {
        selectedItemId = String(id);
        loadVersions();
    }

    function setLabel(set: any) {
        return `${set.name} (${set.id})`;
    }

    onMount(loadSets);
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">Question Bank</h1>
        <p class="text-muted-foreground">Guided workflow: choose set, choose item, then manage versions and compare changes.</p>
    </div>

    <div class="grid gap-3 md:grid-cols-3">
        <Card.Root><Card.Content class="pt-5"><p class="text-xs text-muted-foreground">Sets</p><p class="text-2xl font-semibold">{setCount}</p></Card.Content></Card.Root>
        <Card.Root><Card.Content class="pt-5"><p class="text-xs text-muted-foreground">Items (selected set)</p><p class="text-2xl font-semibold">{itemCount}</p></Card.Content></Card.Root>
        <Card.Root><Card.Content class="pt-5"><p class="text-xs text-muted-foreground">Versions (selected item)</p><p class="text-2xl font-semibold">{versionCount}</p></Card.Content></Card.Root>
    </div>

    <div class="grid gap-6 xl:grid-cols-5">
        <Card.Root class="xl:col-span-3">
            <Card.Header>
                <Card.Title>1) Library Sets</Card.Title>
                <Card.Description>Select a set to work on its items.</Card.Description>
            </Card.Header>
            <Card.Content class="grid gap-3 md:grid-cols-3">
                <div class="space-y-1">
                    <label for="set-tenant-filter" class="text-xs text-muted-foreground">Tenant</label>
                    <Input id="set-tenant-filter" bind:value={filters.tenantId} placeholder="tenant-001" />
                </div>
                <div class="space-y-1">
                    <label for="set-status-filter" class="text-xs text-muted-foreground">Set Status</label>
                    <select id="set-status-filter" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={filters.setStatus}>
                        <option value="">Any</option>
                        {#each SET_ITEM_STATUSES as s}<option value={s}>{s}</option>{/each}
                    </select>
                </div>
                <div class="flex items-end gap-2">
                    <Button variant="outline" onclick={loadSets} disabled={loadingSets}>
                        {#if loadingSets}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{:else}<RefreshCcw class="mr-2 h-4 w-4" />{/if}
                        Reload
                    </Button>
                </div>
            </Card.Content>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row>
                            <Table.Head>Name</Table.Head>
                            <Table.Head>Status</Table.Head>
                            <Table.Head>Owner</Table.Head>
                            <Table.Head>Tag</Table.Head>
                        </Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loadingSets}
                            <Table.Row><Table.Cell colspan={4}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                        {:else if sets.length === 0}
                            <Table.Row><Table.Cell colspan={4}>No sets found</Table.Cell></Table.Row>
                        {:else}
                            {#each sets as s}
                                <Table.Row
                                    class={`cursor-pointer hover:bg-muted/40 ${selectedSetId === String(s.id) ? "bg-muted/60" : ""}`}
                                    onclick={() => selectSet(s.id)}
                                >
                                    <Table.Cell>{setLabel(s)}</Table.Cell>
                                    <Table.Cell><Badge variant={statusBadgeVariant(s.status)}>{s.status}</Badge></Table.Cell>
                                    <Table.Cell>{s.owner || "-"}</Table.Cell>
                                    <Table.Cell>{s.versionTag || "-"}</Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>

        <Card.Root class="xl:col-span-2">
            <Card.Header>
                <Card.Title>Create Set</Card.Title>
                <Card.Description>Use this only when a new library container is needed.</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="space-y-1">
                    <label for="set-tenant" class="text-xs text-muted-foreground">Tenant</label>
                    <Input id="set-tenant" bind:value={setForm.tenantId} />
                </div>
                <div class="space-y-1">
                    <label for="set-name" class="text-xs text-muted-foreground">Set Name</label>
                    <Input id="set-name" bind:value={setForm.name} placeholder="Faculty Evaluation 2026" />
                </div>
                <div class="space-y-1">
                    <label for="set-tag" class="text-xs text-muted-foreground">Version Tag</label>
                    <Input id="set-tag" bind:value={setForm.versionTag} placeholder="v2026.1" />
                </div>
                <div class="space-y-1">
                    <label for="set-owner" class="text-xs text-muted-foreground">Owner</label>
                    <Input id="set-owner" bind:value={setForm.owner} />
                </div>
            </Card.Content>
            <Card.Footer>
                <Button onclick={createSet} disabled={!setForm.name}>
                    <Plus class="mr-2 h-4 w-4" />Create Set
                </Button>
            </Card.Footer>
        </Card.Root>
    </div>

    <div class="grid gap-6 xl:grid-cols-5">
        <Card.Root class="xl:col-span-3">
            <Card.Header>
                <Card.Title>2) Items In Selected Set</Card.Title>
                <Card.Description>Set: {selectedSetId || "-"}</Card.Description>
            </Card.Header>
            <Card.Content class="grid gap-3 md:grid-cols-3">
                <div class="space-y-1 md:col-span-2">
                    <label for="selected-set" class="text-xs text-muted-foreground">Set Selector</label>
                    <Input id="selected-set" bind:value={selectedSetId} list="set-options" placeholder="Search set" onchange={() => loadItems()} />
                </div>
                <div class="space-y-1">
                    <label for="item-status-filter" class="text-xs text-muted-foreground">Item Status</label>
                    <select id="item-status-filter" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={filters.itemStatus}>
                        <option value="">Any</option>
                        {#each SET_ITEM_STATUSES as s}<option value={s}>{s}</option>{/each}
                    </select>
                </div>
            </Card.Content>
            <Card.Content class="pt-0">
                <Button variant="outline" onclick={loadItems} disabled={!selectedSetId || loadingItems}>
                    {#if loadingItems}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                    Load Items
                </Button>
            </Card.Content>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row>
                            <Table.Head>Stable Key</Table.Head>
                            <Table.Head>Type</Table.Head>
                            <Table.Head>Marks</Table.Head>
                            <Table.Head>Active Ver.</Table.Head>
                            <Table.Head>Status</Table.Head>
                        </Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loadingItems}
                            <Table.Row><Table.Cell colspan={5}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                        {:else if items.length === 0}
                            <Table.Row><Table.Cell colspan={5}>No items found</Table.Cell></Table.Row>
                        {:else}
                            {#each items as it}
                                <Table.Row class={`cursor-pointer hover:bg-muted/40 ${selectedItemId === String(it.id) ? "bg-muted/60" : ""}`} onclick={() => selectItem(it.id)}>
                                    <Table.Cell>{it.stableKey}</Table.Cell>
                                    <Table.Cell>{it.defaultType}</Table.Cell>
                                    <Table.Cell>{it.defaultMarks}</Table.Cell>
                                    <Table.Cell>{it.activeVersionNo}</Table.Cell>
                                    <Table.Cell><Badge variant={statusBadgeVariant(it.status)}>{it.status}</Badge></Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>

        <Card.Root class="xl:col-span-2">
            <Card.Header>
                <Card.Title>Create Item</Card.Title>
                <Card.Description>Creates a logical question key in the selected set.</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="space-y-1">
                    <label for="item-stable-key" class="text-xs text-muted-foreground">Stable Key</label>
                    <Input id="item-stable-key" bind:value={itemForm.stableKey} placeholder="FAC_PEER_TEACHING_CLARITY" />
                </div>
                <div class="space-y-1">
                    <label for="item-context" class="text-xs text-muted-foreground">Context Type</label>
                    <Input id="item-context" bind:value={itemForm.contextType} placeholder="FACULTY" />
                </div>
                <div class="space-y-1">
                    <label for="item-category" class="text-xs text-muted-foreground">Category</label>
                    <Input id="item-category" bind:value={itemForm.categoryName} placeholder="Instruction" />
                </div>
                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="item-type" class="text-xs text-muted-foreground">Default Type</label>
                        <select id="item-type" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={itemForm.defaultType}>
                            {#each QUESTION_TYPES as t}<option value={t}>{t}</option>{/each}
                        </select>
                    </div>
                    <div class="space-y-1">
                        <label for="item-marks" class="text-xs text-muted-foreground">Default Marks</label>
                        <Input id="item-marks" type="number" min="0" bind:value={itemForm.defaultMarks} />
                    </div>
                </div>
            </Card.Content>
            <Card.Footer>
                <Button onclick={createItem} disabled={!selectedSetId || !itemForm.stableKey}>Create Item</Button>
            </Card.Footer>
        </Card.Root>
    </div>

    <div class="grid gap-6 xl:grid-cols-5">
        <Card.Root class="xl:col-span-3">
            <Card.Header>
                <Card.Title>3) Version Studio</Card.Title>
                <Card.Description>Item: {selectedItemId || "-"}</Card.Description>
            </Card.Header>
            <Card.Content class="grid gap-3 md:grid-cols-3">
                <div class="space-y-1 md:col-span-2">
                    <label for="selected-item" class="text-xs text-muted-foreground">Item Selector</label>
                    <Input id="selected-item" bind:value={selectedItemId} list="item-options" placeholder="Search item" onchange={() => loadVersions()} />
                </div>
                <div class="space-y-1">
                    <label for="version-status-filter" class="text-xs text-muted-foreground">Version Status</label>
                    <select id="version-status-filter" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={filters.versionStatus}>
                        <option value="">Any</option>
                        {#each VERSION_STATUSES as s}<option value={s}>{s}</option>{/each}
                    </select>
                </div>
            </Card.Content>
            <Card.Content class="pt-0">
                <Button variant="outline" onclick={loadVersions} disabled={!selectedItemId || loadingVersions}>
                    {#if loadingVersions}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                    Load Versions
                </Button>
            </Card.Content>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row>
                            <Table.Head>Version</Table.Head>
                            <Table.Head>Status</Table.Head>
                            <Table.Head>Type</Table.Head>
                            <Table.Head>Marks</Table.Head>
                            <Table.Head>Remarks</Table.Head>
                            <Table.Head>Action</Table.Head>
                        </Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loadingVersions}
                            <Table.Row><Table.Cell colspan={6}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                        {:else if versions.length === 0}
                            <Table.Row><Table.Cell colspan={6}>No versions found</Table.Cell></Table.Row>
                        {:else}
                            {#each versions as v}
                                <Table.Row>
                                    <Table.Cell>{v.versionNo}</Table.Cell>
                                    <Table.Cell><Badge variant={statusBadgeVariant(v.status)}>{v.status}</Badge></Table.Cell>
                                    <Table.Cell>{v.questionType}</Table.Cell>
                                    <Table.Cell>{v.marks}</Table.Cell>
                                    <Table.Cell>{v.remarksMandatory ? "Required" : "Optional"}</Table.Cell>
                                    <Table.Cell>
                                        <Button size="sm" variant="outline" onclick={() => activateVersion(v.versionNo)} disabled={v.status === "ACTIVE"}>Activate</Button>
                                    </Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>

        <Card.Root class="xl:col-span-2">
            <Card.Header>
                <Card.Title>Create Version</Card.Title>
                <Card.Description>Creates a revision for the selected item.</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="version-status" class="text-xs text-muted-foreground">Status</label>
                        <select id="version-status" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={versionForm.status}>
                            {#each VERSION_STATUSES as s}<option value={s}>{s}</option>{/each}
                        </select>
                    </div>
                    <div class="space-y-1">
                        <label for="version-type" class="text-xs text-muted-foreground">Question Type</label>
                        <select id="version-type" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={versionForm.questionType}>
                            {#each QUESTION_TYPES as t}<option value={t}>{t}</option>{/each}
                        </select>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="version-marks" class="text-xs text-muted-foreground">Marks</label>
                        <Input id="version-marks" type="number" min="0" bind:value={versionForm.marks} />
                    </div>
                    <div class="space-y-1">
                        <label for="version-change-summary" class="text-xs text-muted-foreground">Change Summary</label>
                        <Input id="version-change-summary" bind:value={versionForm.changeSummary} placeholder="Updated wording" />
                    </div>
                </div>

                <div class="space-y-1">
                    <label for="version-question-text" class="text-xs text-muted-foreground">Question Text</label>
                    <Textarea id="version-question-text" bind:value={versionForm.questionText} rows={4} placeholder="Enter final question text" />
                </div>

                <div class="flex items-center gap-2 text-sm">
                    <input id="remarks-mandatory" type="checkbox" bind:checked={versionForm.remarksMandatory} />
                    <label for="remarks-mandatory">Remarks mandatory</label>
                </div>

                <div class="space-y-2">
                    <p class="text-xs font-medium text-muted-foreground">Metadata (key-value)</p>
                    {#each metadataRows as row, i}
                        <div class="grid grid-cols-5 gap-2">
                            <Input bind:value={row.key} placeholder="key" class="col-span-2" />
                            <Input bind:value={row.value} placeholder='value or JSON (e.g. ["A","B"])' class="col-span-2" />
                            <Button type="button" variant="outline" onclick={() => removeMetadataRow(i)}>Remove</Button>
                        </div>
                    {/each}
                    <Button type="button" size="sm" variant="outline" onclick={addMetadataRow}>Add Metadata Row</Button>
                </div>
            </Card.Content>
            <Card.Footer class="flex gap-2">
                <Button onclick={createVersion} disabled={!selectedItemId || !versionForm.questionText}>Create Version</Button>
                <Button variant="outline" onclick={resetVersionForm}>Reset</Button>
            </Card.Footer>
        </Card.Root>
    </div>

    <Card.Root>
        <Card.Header>
            <Card.Title>4) Compare Versions</Card.Title>
            <Card.Description>Compare two revisions of the selected item.</Card.Description>
        </Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-4">
            <div class="space-y-1">
                <label for="compare-item" class="text-xs text-muted-foreground">Item</label>
                <Input id="compare-item" bind:value={selectedItemId} list="item-options" onchange={() => loadVersions()} />
            </div>
            <div class="space-y-1">
                <label for="compare-from" class="text-xs text-muted-foreground">From Version</label>
                <select id="compare-from" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={compareForm.fromVersion}>
                    {#each versions as v}<option value={v.versionNo}>{v.versionNo}</option>{/each}
                </select>
            </div>
            <div class="space-y-1">
                <label for="compare-to" class="text-xs text-muted-foreground">To Version</label>
                <select id="compare-to" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={compareForm.toVersion}>
                    {#each versions as v}<option value={v.versionNo}>{v.versionNo}</option>{/each}
                </select>
            </div>
            <div class="flex items-end">
                <Button onclick={compareVersions} disabled={!selectedItemId || loadingCompare}>
                    {#if loadingCompare}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                    Compare
                </Button>
            </div>
        </Card.Content>
        {#if compareResponse}
            <Card.Content>
                <div class="rounded-md border bg-muted/30 p-3">
                    <DataView data={compareResponse} />
                </div>
            </Card.Content>
        {/if}
    </Card.Root>

    <datalist id="set-options">
        {#each sets as s}
            <option value={s.id}>{setLabel(s)}</option>
        {/each}
    </datalist>

    <datalist id="item-options">
        {#each items as it}
            <option value={it.id}>{it.stableKey}</option>
        {/each}
    </datalist>
</div>
