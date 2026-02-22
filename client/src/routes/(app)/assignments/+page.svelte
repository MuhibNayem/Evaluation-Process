<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import DataView from "$lib/components/data-view.svelte";
    import { Loader2, RefreshCcw, Search, Sparkles } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let loading = $state(false);
    let loadingById = $state(false);
    let loadingCampaigns = $state(false);

    let filters = $state({
        campaignId: "",
        evaluatorId: "",
        evaluateeId: "",
        stepType: "",
        sectionId: "",
        facultyId: "",
        status: "",
        page: 0,
        size: 20,
        sortBy: "updatedAt",
        sortDir: "desc",
    });

    let result = $state<any>({
        items: [],
        page: 0,
        size: 20,
        totalItems: 0,
        totalPages: 0,
    });
    let selectedAssignment = $state<any>(null);

    let campaigns = $state<any[]>([]);
    let assignmentOptions = $state<string[]>([]);
    let evaluatorOptions = $state<string[]>([]);
    let evaluateeOptions = $state<string[]>([]);

    let form = $state({
        id: "",
        campaignId: "",
        evaluatorId: "",
        evaluateeId: "",
        evaluatorRole: "PEER",
        stepType: "",
        sectionId: "",
        facultyId: "",
        anonymityMode: "",
        status: "ACTIVE",
    });

    async function loadCampaigns() {
        loadingCampaigns = true;
        try {
            const res = await api.get("/campaigns", { params: { page: 0, size: 300 } });
            campaigns = Array.isArray(res.data) ? res.data : [];
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load campaigns");
        } finally {
            loadingCampaigns = false;
        }
    }

    function refreshIdentityOptions(items: any[]) {
        const assignmentIds = new Set<string>(assignmentOptions);
        const evaluators = new Set<string>(evaluatorOptions);
        const evaluatees = new Set<string>(evaluateeOptions);

        for (const item of items || []) {
            if (item?.id) assignmentIds.add(String(item.id));
            if (item?.evaluatorId) evaluators.add(String(item.evaluatorId));
            if (item?.evaluateeId) evaluatees.add(String(item.evaluateeId));
        }

        if (selectedAssignment?.id) assignmentIds.add(String(selectedAssignment.id));
        if (selectedAssignment?.evaluatorId) evaluators.add(String(selectedAssignment.evaluatorId));
        if (selectedAssignment?.evaluateeId) evaluatees.add(String(selectedAssignment.evaluateeId));

        assignmentOptions = Array.from(assignmentIds).sort();
        evaluatorOptions = Array.from(evaluators).sort();
        evaluateeOptions = Array.from(evaluatees).sort();
    }

    async function loadAssignments() {
        loading = true;
        try {
            const params: Record<string, unknown> = {
                page: filters.page,
                size: filters.size,
                sortBy: filters.sortBy,
                sortDir: filters.sortDir,
            };
            for (const [k, v] of Object.entries(filters)) {
                if (["page", "size", "sortBy", "sortDir"].includes(k)) continue;
                if (String(v ?? "").trim()) params[k] = String(v).trim();
            }
            const res = await api.get("/assignments", { params });
            result = res.data;
            refreshIdentityOptions(res.data?.items || []);
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load assignments");
        } finally {
            loading = false;
        }
    }

    onMount(async () => {
        await Promise.all([loadCampaigns(), loadAssignments()]);
    });

    function formatDate(value: string | null | undefined) {
        if (!value) return "-";
        return new Date(value).toLocaleString();
    }

    function resetFilters() {
        filters = {
            ...filters,
            campaignId: "",
            evaluatorId: "",
            evaluateeId: "",
            stepType: "",
            sectionId: "",
            facultyId: "",
            status: "",
            page: 0,
        };
        loadAssignments();
    }

    function resetForm() {
        form = {
            id: "",
            campaignId: "",
            evaluatorId: "",
            evaluateeId: "",
            evaluatorRole: "PEER",
            stepType: "",
            sectionId: "",
            facultyId: "",
            anonymityMode: "",
            status: "ACTIVE",
        };
        selectedAssignment = null;
    }

    function hydrateFormFromAssignment(data: any) {
        form = {
            ...form,
            id: data.id || "",
            campaignId: data.campaignId || "",
            evaluatorId: data.evaluatorId || "",
            evaluateeId: data.evaluateeId || "",
            evaluatorRole: data.evaluatorRole || "PEER",
            stepType: data.stepType || "",
            sectionId: data.sectionId || "",
            facultyId: data.facultyId || "",
            anonymityMode: data.anonymityMode || "",
            status: data.status || "ACTIVE",
        };
    }

    async function loadById() {
        if (!form.id.trim()) return;
        loadingById = true;
        try {
            const res = await api.get(`/assignments/${form.id.trim()}`);
            selectedAssignment = res.data;
            hydrateFormFromAssignment(res.data);
            refreshIdentityOptions([res.data]);
            toast.success("Assignment loaded");
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load assignment");
        } finally {
            loadingById = false;
        }
    }

    function isInOptions(value: string, options: string[]) {
        return options.includes((value || "").trim());
    }

    function validateComboboxSelectionsForCreate() {
        if (!isInOptions(form.campaignId, campaigns.map((c) => String(c.id)))) {
            toast.error("Select a valid campaign from the search list");
            return false;
        }
        if (evaluatorOptions.length === 0) {
            toast.error("No evaluator options available. Load assignments or campaign data first.");
            return false;
        }
        if (!isInOptions(form.evaluatorId, evaluatorOptions)) {
            toast.error("Select a valid evaluator from the search list");
            return false;
        }
        if (evaluateeOptions.length === 0) {
            toast.error("No evaluatee options available. Load assignments or campaign data first.");
            return false;
        }
        if (!isInOptions(form.evaluateeId, evaluateeOptions)) {
            toast.error("Select a valid evaluatee from the search list");
            return false;
        }
        return true;
    }

    async function createAssignment() {
        if (!validateComboboxSelectionsForCreate()) return;
        try {
            const payload = {
                campaignId: form.campaignId,
                evaluatorId: form.evaluatorId,
                evaluateeId: form.evaluateeId,
                evaluatorRole: form.evaluatorRole,
                stepType: form.stepType || null,
                sectionId: form.sectionId || null,
                facultyId: form.facultyId || null,
                anonymityMode: form.anonymityMode || null,
                status: form.status || null,
            };
            const res = await api.post("/assignments", payload);
            selectedAssignment = res.data;
            form.id = res.data.id;
            refreshIdentityOptions([res.data]);
            toast.success("Assignment created");
            await loadAssignments();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to create assignment");
        }
    }

    async function updateAssignment() {
        if (!form.id.trim()) return;
        try {
            const payload = {
                stepType: form.stepType || null,
                sectionId: form.sectionId || null,
                facultyId: form.facultyId || null,
                anonymityMode: form.anonymityMode || null,
                status: form.status || null,
            };
            const res = await api.put(`/assignments/${form.id.trim()}`, payload);
            selectedAssignment = res.data;
            hydrateFormFromAssignment(res.data);
            refreshIdentityOptions([res.data]);
            toast.success("Assignment updated");
            await loadAssignments();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to update assignment");
        }
    }

    async function selectRow(item: any) {
        selectedAssignment = item;
        hydrateFormFromAssignment(item);
        refreshIdentityOptions([item]);
    }

    function campaignLabel(id: string) {
        const campaign = campaigns.find((c) => String(c.id) === String(id));
        if (!campaign) return id;
        return `${campaign.name} (${campaign.id})`;
    }

    function statusVariant(status: string) {
        switch ((status || "").toUpperCase()) {
            case "COMPLETED":
                return "secondary";
            case "INACTIVE":
                return "destructive";
            default:
                return "default";
        }
    }

    const activeCount = $derived(result.items?.filter((i: any) => i.status === "ACTIVE").length ?? 0);
    const completedCount = $derived(result.items?.filter((i: any) => i.status === "COMPLETED").length ?? 0);
    const inactiveCount = $derived(result.items?.filter((i: any) => i.status === "INACTIVE").length ?? 0);
</script>

<div class="space-y-6 pb-20">
    <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
        <div>
            <h1 class="text-3xl font-bold tracking-tight">Assignments</h1>
            <p class="text-muted-foreground">Manage assignment records with search-first selectors and guided create/update actions.</p>
        </div>
        <div class="flex items-center gap-2">
            <Button variant="outline" onclick={loadCampaigns} disabled={loadingCampaigns}>
                {#if loadingCampaigns}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                Refresh Lookup Data
            </Button>
            <Button variant="outline" onclick={resetForm}>
                New Form
            </Button>
        </div>
    </div>

    <div class="grid gap-3 md:grid-cols-4">
        <Card.Root>
            <Card.Content class="pt-5">
                <p class="text-xs text-muted-foreground">Visible Rows</p>
                <p class="text-2xl font-semibold">{result.items?.length ?? 0}</p>
            </Card.Content>
        </Card.Root>
        <Card.Root>
            <Card.Content class="pt-5">
                <p class="text-xs text-muted-foreground">Active</p>
                <p class="text-2xl font-semibold">{activeCount}</p>
            </Card.Content>
        </Card.Root>
        <Card.Root>
            <Card.Content class="pt-5">
                <p class="text-xs text-muted-foreground">Completed</p>
                <p class="text-2xl font-semibold">{completedCount}</p>
            </Card.Content>
        </Card.Root>
        <Card.Root>
            <Card.Content class="pt-5">
                <p class="text-xs text-muted-foreground">Inactive</p>
                <p class="text-2xl font-semibold">{inactiveCount}</p>
            </Card.Content>
        </Card.Root>
    </div>

    <Card.Root>
        <Card.Header>
            <Card.Title>Filters</Card.Title>
            <Card.Description>Use search-enabled combobox inputs to narrow down the assignment list quickly.</Card.Description>
        </Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-4">
            <div class="space-y-1 md:col-span-2">
                <label for="filter-campaign" class="text-xs text-muted-foreground">Campaign</label>
                <Input id="filter-campaign" bind:value={filters.campaignId} list="campaign-options" placeholder="Search campaign" />
            </div>
            <div class="space-y-1">
                <label for="filter-evaluator" class="text-xs text-muted-foreground">Evaluator</label>
                <Input id="filter-evaluator" bind:value={filters.evaluatorId} list="evaluator-options" placeholder="Search evaluator" />
            </div>
            <div class="space-y-1">
                <label for="filter-evaluatee" class="text-xs text-muted-foreground">Evaluatee</label>
                <Input id="filter-evaluatee" bind:value={filters.evaluateeId} list="evaluatee-options" placeholder="Search evaluatee" />
            </div>

            <div class="space-y-1">
                <label for="filter-step-type" class="text-xs text-muted-foreground">Step Type</label>
                <select id="filter-step-type" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={filters.stepType}>
                    <option value="">Any</option>
                    <option value="STUDENT">STUDENT</option>
                    <option value="PEER">PEER</option>
                    <option value="SELF">SELF</option>
                    <option value="DEPARTMENT">DEPARTMENT</option>
                </select>
            </div>
            <div class="space-y-1">
                <label for="filter-status" class="text-xs text-muted-foreground">Status</label>
                <select id="filter-status" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={filters.status}>
                    <option value="">Any</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="COMPLETED">COMPLETED</option>
                    <option value="INACTIVE">INACTIVE</option>
                </select>
            </div>
            <div class="space-y-1">
                <label for="filter-section-id" class="text-xs text-muted-foreground">Section</label>
                <Input id="filter-section-id" bind:value={filters.sectionId} placeholder="e.g. CSE-221-A" />
            </div>
            <div class="space-y-1">
                <label for="filter-faculty-id" class="text-xs text-muted-foreground">Faculty</label>
                <Input id="filter-faculty-id" bind:value={filters.facultyId} placeholder="e.g. ENG" />
            </div>
        </Card.Content>
        <Card.Footer class="flex flex-wrap gap-2">
            <Button onclick={loadAssignments} disabled={loading}>
                {#if loading}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{:else}<Search class="mr-2 h-4 w-4" />{/if}
                Search
            </Button>
            <Button variant="outline" onclick={resetFilters} disabled={loading}>
                <RefreshCcw class="mr-2 h-4 w-4" />Reset Filters
            </Button>
            <div class="flex items-center gap-2 text-xs text-muted-foreground">
                <span>Page</span>
                <Input type="number" bind:value={filters.page} min="0" class="w-20" />
                <span>Size</span>
                <Input type="number" bind:value={filters.size} min="1" class="w-20" />
            </div>
        </Card.Footer>
    </Card.Root>

    <div class="grid gap-6 lg:grid-cols-3">
        <Card.Root class="lg:col-span-2">
            <Card.Header>
                <Card.Title>Assignments</Card.Title>
                <Card.Description>
                    {result.totalItems ?? 0} total items, page {result.page ?? 0} of {result.totalPages ?? 0}. Click a row to load it in the editor.
                </Card.Description>
            </Card.Header>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row>
                            <Table.Head>ID</Table.Head>
                            <Table.Head>Campaign</Table.Head>
                            <Table.Head>Evaluator</Table.Head>
                            <Table.Head>Evaluatee</Table.Head>
                            <Table.Head>Role</Table.Head>
                            <Table.Head>Step</Table.Head>
                            <Table.Head>Status</Table.Head>
                            <Table.Head>Updated</Table.Head>
                        </Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loading}
                            <Table.Row>
                                <Table.Cell colspan={8}>
                                    <div class="flex items-center gap-2 py-3 text-sm text-muted-foreground">
                                        <Loader2 class="h-4 w-4 animate-spin" /> Loading assignments...
                                    </div>
                                </Table.Cell>
                            </Table.Row>
                        {:else if !result.items || result.items.length === 0}
                            <Table.Row><Table.Cell colspan={8}>No assignments found for current filters.</Table.Cell></Table.Row>
                        {:else}
                            {#each result.items as item}
                                <Table.Row
                                    class={`cursor-pointer transition-colors hover:bg-muted/40 ${selectedAssignment?.id === item.id ? "bg-muted/60" : ""}`}
                                    onclick={() => selectRow(item)}
                                >
                                    <Table.Cell class="font-mono text-xs">{item.id}</Table.Cell>
                                    <Table.Cell>{campaignLabel(item.campaignId)}</Table.Cell>
                                    <Table.Cell>{item.evaluatorId}</Table.Cell>
                                    <Table.Cell>{item.evaluateeId}</Table.Cell>
                                    <Table.Cell>{item.evaluatorRole}</Table.Cell>
                                    <Table.Cell>{item.stepType || "-"}</Table.Cell>
                                    <Table.Cell>
                                        <Badge variant={statusVariant(item.status)}>{item.status}</Badge>
                                    </Table.Cell>
                                    <Table.Cell>{formatDate(item.updatedAt)}</Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <Card.Title class="flex items-center gap-2">
                    <Sparkles class="h-4 w-4" /> Assignment Editor
                </Card.Title>
                <Card.Description>
                    Create a new assignment or update metadata for a selected one.
                </Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="space-y-1">
                    <label for="form-assignment-id" class="text-xs text-muted-foreground">Assignment</label>
                    <Input id="form-assignment-id" bind:value={form.id} list="assignment-options" placeholder="Search assignment ID" />
                </div>

                <div class="space-y-1">
                    <label for="form-campaign-id" class="text-xs text-muted-foreground">Campaign</label>
                    <Input id="form-campaign-id" bind:value={form.campaignId} list="campaign-options" placeholder="Search campaign" />
                </div>

                <div class="space-y-1">
                    <label for="form-evaluator-id" class="text-xs text-muted-foreground">Evaluator</label>
                    <Input id="form-evaluator-id" bind:value={form.evaluatorId} list="evaluator-options" placeholder="Search evaluator" />
                </div>

                <div class="space-y-1">
                    <label for="form-evaluatee-id" class="text-xs text-muted-foreground">Evaluatee</label>
                    <Input id="form-evaluatee-id" bind:value={form.evaluateeId} list="evaluatee-options" placeholder="Search evaluatee" />
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="form-evaluator-role" class="text-xs text-muted-foreground">Role</label>
                        <select id="form-evaluator-role" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={form.evaluatorRole}>
                            <option value="PEER">PEER</option>
                            <option value="STUDENT">STUDENT</option>
                            <option value="SELF">SELF</option>
                            <option value="DEPARTMENT">DEPARTMENT</option>
                            <option value="SUPERVISOR">SUPERVISOR</option>
                            <option value="MANAGER">MANAGER</option>
                        </select>
                    </div>
                    <div class="space-y-1">
                        <label for="form-step-type" class="text-xs text-muted-foreground">Step</label>
                        <select id="form-step-type" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={form.stepType}>
                            <option value="">None</option>
                            <option value="STUDENT">STUDENT</option>
                            <option value="PEER">PEER</option>
                            <option value="SELF">SELF</option>
                            <option value="DEPARTMENT">DEPARTMENT</option>
                        </select>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="form-section-id" class="text-xs text-muted-foreground">Section</label>
                        <Input id="form-section-id" bind:value={form.sectionId} placeholder="Section ID" />
                    </div>
                    <div class="space-y-1">
                        <label for="form-faculty-id" class="text-xs text-muted-foreground">Faculty</label>
                        <Input id="form-faculty-id" bind:value={form.facultyId} placeholder="Faculty ID" />
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-2">
                    <div class="space-y-1">
                        <label for="form-anonymity-mode" class="text-xs text-muted-foreground">Anonymity</label>
                        <select id="form-anonymity-mode" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={form.anonymityMode}>
                            <option value="">None</option>
                            <option value="VISIBLE">VISIBLE</option>
                            <option value="ANONYMOUS">ANONYMOUS</option>
                        </select>
                    </div>
                    <div class="space-y-1">
                        <label for="form-status" class="text-xs text-muted-foreground">Status</label>
                        <select id="form-status" class="h-9 w-full rounded-md border bg-background px-3 text-sm" bind:value={form.status}>
                            <option value="ACTIVE">ACTIVE</option>
                            <option value="COMPLETED">COMPLETED</option>
                            <option value="INACTIVE">INACTIVE</option>
                        </select>
                    </div>
                </div>
            </Card.Content>
            <Card.Footer class="flex flex-wrap gap-2">
                <Button variant="outline" onclick={loadById} disabled={!form.id.trim() || loadingById}>
                    {#if loadingById}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                    Load
                </Button>
                <Button onclick={createAssignment} disabled={!form.campaignId || !form.evaluatorId || !form.evaluateeId}>Create</Button>
                <Button variant="secondary" onclick={updateAssignment} disabled={!form.id.trim()}>Update</Button>
            </Card.Footer>
        </Card.Root>
    </div>

    {#if selectedAssignment}
        <Card.Root>
            <Card.Header>
                <Card.Title>Selected Assignment</Card.Title>
                <Card.Description>Current server payload for the selected row.</Card.Description>
            </Card.Header>
            <Card.Content>
                <div class="rounded-md border bg-muted/30 p-3">
                    <DataView data={selectedAssignment} />
                </div>
            </Card.Content>
        </Card.Root>
    {/if}

    <datalist id="campaign-options">
        {#each campaigns as campaign}
            <option value={campaign.id}>{campaignLabel(campaign.id)}</option>
        {/each}
    </datalist>

    <datalist id="assignment-options">
        {#each assignmentOptions as assignmentId}
            <option value={assignmentId}></option>
        {/each}
    </datalist>

    <datalist id="evaluator-options">
        {#each evaluatorOptions as evaluatorId}
            <option value={evaluatorId}></option>
        {/each}
    </datalist>

    <datalist id="evaluatee-options">
        {#each evaluateeOptions as evaluateeId}
            <option value={evaluateeId}></option>
        {/each}
    </datalist>
</div>
