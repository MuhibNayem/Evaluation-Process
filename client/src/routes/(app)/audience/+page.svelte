<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { DEFAULT_TENANT_ID } from "$lib/config.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import { Switch } from "$lib/components/ui/switch/index.js";
    import { Loader2, RefreshCcw, Plus, Trash2 } from "@lucide/svelte";
    import HelpInfo from "$lib/components/help-info.svelte";
    import { toast } from "svelte-sonner";

    const AUDIENCE_SOURCE_TYPES = ["JSON", "CSV", "REST", "JDBC"] as const;
    type AudienceSourceType = (typeof AUDIENCE_SOURCE_TYPES)[number];

    let tenantId = $state(DEFAULT_TENANT_ID);

    let ingestionForm = $state({
        sourceType: "JSON",
        mappingProfileId: "",
        dryRun: true,
        sourceConfigText: '{"entityType":"PERSON","records":[]}',
    });

    let mappingForm = $state({
        profileId: "",
        name: "",
        sourceType: "JSON",
        active: true,
        fieldMappingsText: '{"person_id":"id","display_name":"name","email":"email"}',
    });
    let advancedIngestionJson = $state(false);
    let advancedMappingsJson = $state(false);
    let jsonEntityType = $state<"PERSON" | "GROUP" | "MEMBERSHIP">("PERSON");
    let jsonRecordRows = $state<{ id: string; fields: Record<string, string> }[]>([
        {
            id: crypto.randomUUID(),
            fields: {
                person_id: "u1",
                display_name: "Alice",
                email: "alice@example.com",
            },
        },
    ]);
    let sourceConfigRows = $state<{ id: string; key: string; value: string }[]>([
        { id: crypto.randomUUID(), key: "entityType", value: "PERSON" },
    ]);
    let mappingRows = $state<{ id: string; source: string; target: string }[]>([
        { id: crypto.randomUUID(), source: "person_id", target: "id" },
        { id: crypto.randomUUID(), source: "display_name", target: "name" },
        { id: crypto.randomUUID(), source: "email", target: "email" },
    ]);

    let mappingValidationText = $state("");
    let profileEventLimit = $state(50);
    let rejectionLimit = $state(200);

    let isBusy = $state(false);
    let runsLoading = $state(false);
    let profilesLoading = $state(false);

    let runs = $state<any[]>([]);
    let selectedRunId = $state("");
    let selectedRun: any = $state(null);
    let runRejections = $state<any[]>([]);

    let profiles = $state<any[]>([]);
    let selectedProfileId = $state<string>("");
    let profileEvents = $state<any[]>([]);

    let lastIngestionResponse = $state<any>(null);
    let lastValidationResponse = $state<any>(null);

    function tryParseJson(input: string, fieldName: string) {
        try {
            return JSON.parse(input || "{}");
        } catch {
            throw new Error(`${fieldName} must be valid JSON`);
        }
    }

    function parseScalar(value: string): string | number | boolean {
        const trimmed = value.trim();
        if (
            (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        ) {
            try {
                return JSON.parse(trimmed) as any;
            } catch {
                // Fall back to string when not valid JSON.
            }
        }
        if (trimmed.toLowerCase() === "true") return true;
        if (trimmed.toLowerCase() === "false") return false;
        if (/^-?\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
        return value;
    }

    function rowsToObject(rows: { key: string; value: string }[]) {
        const out: Record<string, unknown> = {};
        for (const row of rows) {
            if (!row.key.trim()) continue;
            out[row.key.trim()] = parseScalar(row.value);
        }
        return out;
    }

    function rowsToMappings(rows: { source: string; target: string }[]) {
        const out: Record<string, string> = {};
        for (const row of rows) {
            if (!row.source.trim() || !row.target.trim()) continue;
            out[row.source.trim()] = row.target.trim();
        }
        return out;
    }

    function addSourceConfigRow() {
        sourceConfigRows = [...sourceConfigRows, { id: crypto.randomUUID(), key: "", value: "" }];
    }

    function removeSourceConfigRow(id: string) {
        sourceConfigRows = sourceConfigRows.filter((r) => r.id !== id);
    }

    function addMappingRow() {
        mappingRows = [...mappingRows, { id: crypto.randomUUID(), source: "", target: "" }];
    }

    function removeMappingRow(id: string) {
        mappingRows = mappingRows.filter((r) => r.id !== id);
    }

    function jsonFieldKeys(entityType: "PERSON" | "GROUP" | "MEMBERSHIP"): string[] {
        if (entityType === "GROUP") {
            return ["group_id", "group_type", "group_name", "parent_group_id", "external_ref", "active"];
        }
        if (entityType === "MEMBERSHIP") {
            return ["person_id", "group_id", "role", "valid_from", "valid_to", "active"];
        }
        return ["person_id", "display_name", "email", "active"];
    }

    function emptyJsonRecord(entityType: "PERSON" | "GROUP" | "MEMBERSHIP"): Record<string, string> {
        const out: Record<string, string> = {};
        for (const key of jsonFieldKeys(entityType)) {
            out[key] = "";
        }
        return out;
    }

    function addJsonRecordRow() {
        jsonRecordRows = [...jsonRecordRows, { id: crypto.randomUUID(), fields: emptyJsonRecord(jsonEntityType) }];
    }

    function removeJsonRecordRow(id: string) {
        jsonRecordRows = jsonRecordRows.filter((row) => row.id !== id);
    }

    function updateJsonEntityType(nextType: string) {
        const next = (nextType.toUpperCase() as "PERSON" | "GROUP" | "MEMBERSHIP");
        jsonEntityType = next;
        const keys = jsonFieldKeys(next);
        jsonRecordRows = jsonRecordRows.map((row) => {
            const fields: Record<string, string> = {};
            for (const key of keys) {
                fields[key] = row.fields[key] ?? "";
            }
            return { ...row, fields };
        });
    }

    function buildJsonSourceConfigFromBuilder() {
        const records = jsonRecordRows
            .map((row) => {
                const item: Record<string, unknown> = {};
                for (const [key, value] of Object.entries(row.fields)) {
                    if (!value.trim()) continue;
                    item[key] = parseScalar(value);
                }
                return item;
            })
            .filter((row) => Object.keys(row).length > 0);
        return {
            entityType: jsonEntityType,
            records,
        };
    }

    function normalizeAudienceSourceType(sourceType: string): AudienceSourceType {
        const normalized = sourceType.trim().toUpperCase();
        return (AUDIENCE_SOURCE_TYPES.includes(normalized as AudienceSourceType)
            ? normalized
            : "JSON") as AudienceSourceType;
    }

    function ingestionPreset(sourceType: AudienceSourceType): {
        rows: Array<{ key: string; value: string }>;
        config: Record<string, unknown>;
        required: string;
        example: string;
    } {
        if (sourceType === "CSV") {
            return {
                rows: [
                    {
                        key: "csvData",
                        value: "person_id,display_name,email\nu1,Alice,alice@example.com",
                    },
                ],
                config: {
                    csvData: "person_id,display_name,email\nu1,Alice,alice@example.com",
                },
                required: "Required key: csvData",
                example: "csvData with header row + data rows",
            };
        }
        if (sourceType === "REST") {
            return {
                rows: [
                    { key: "url", value: "https://example.com/api/audience" },
                    { key: "method", value: "GET" },
                    { key: "recordsPath", value: "records" },
                    { key: "connectTimeoutMs", value: "3000" },
                    { key: "readTimeoutMs", value: "10000" },
                ],
                config: {
                    url: "https://example.com/api/audience",
                    method: "GET",
                    recordsPath: "records",
                    connectTimeoutMs: 3000,
                    readTimeoutMs: 10000,
                },
                required: "Required key: url (optional: method, headers, body, recordsPath, timeout ms)",
                example: "url=https://... method=GET recordsPath=records",
            };
        }
        if (sourceType === "JDBC") {
            return {
                rows: [
                    { key: "connectionRef", value: "hrReadReplica" },
                    {
                        key: "query",
                        value: "select person_id, display_name, email from audience_people where active = true",
                    },
                ],
                config: {
                    connectionRef: "hrReadReplica",
                    query: "select person_id, display_name, email from audience_people where active = true",
                },
                required: "Required key: connectionRef (query required unless defaultQuery exists)",
                example: "connectionRef=hrReadReplica, query=SELECT ...",
            };
        }
        return {
            rows: [
                { key: "entityType", value: "PERSON" },
                { key: "records", value: '[{"person_id":"u1","display_name":"Alice","email":"alice@example.com"}]' },
            ],
            config: {
                entityType: "PERSON",
                records: [{ person_id: "u1", display_name: "Alice", email: "alice@example.com" }],
            },
            required: "Required key: records (JSON array). Optional: entityType (PERSON/GROUP/MEMBERSHIP; default PERSON).",
            example:
                'entityType=PERSON, records=[{"person_id":"u1","display_name":"Alice","email":"alice@example.com"}]',
        };
    }

    function applyIngestionSourceTypePreset(nextType: string) {
        const normalized = normalizeAudienceSourceType(nextType);
        ingestionForm.sourceType = normalized;
        const preset = ingestionPreset(normalized);
        sourceConfigRows = preset.rows.map((row) => ({ id: crypto.randomUUID(), ...row }));
        ingestionForm.sourceConfigText = JSON.stringify(preset.config, null, 2);
        if (normalized === "JSON") {
            jsonEntityType = "PERSON";
            jsonRecordRows = [
                {
                    id: crypto.randomUUID(),
                    fields: {
                        person_id: "u1",
                        display_name: "Alice",
                        email: "alice@example.com",
                        active: "true",
                    },
                },
            ];
        }
    }

    function sourceTypeRequiredText(sourceType: string): string {
        return ingestionPreset(normalizeAudienceSourceType(sourceType)).required;
    }

    function sourceTypeExampleText(sourceType: string): string {
        return ingestionPreset(normalizeAudienceSourceType(sourceType)).example;
    }

    function applyMappingSourceType(nextType: string) {
        mappingForm.sourceType = normalizeAudienceSourceType(nextType);
    }

    async function loadRuns() {
        runsLoading = true;
        try {
            const res = await api.get("/audience/ingestion-runs", {
                params: { tenantId, limit: 50 },
            });
            runs = res.data;
            if (!selectedRunId && runs.length > 0) {
                selectedRunId = runs[0].id;
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load ingestion runs");
        } finally {
            runsLoading = false;
        }
    }

    async function loadProfiles() {
        profilesLoading = true;
        try {
            const res = await api.get("/audience/mapping-profiles", {
                params: { tenantId },
            });
            profiles = res.data;
            if (!selectedProfileId && profiles.length > 0) {
                selectedProfileId = String(profiles[0].id);
            }
            if (!ingestionForm.mappingProfileId && profiles.length > 0) {
                ingestionForm.mappingProfileId = String(profiles[0].id);
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load mapping profiles");
        } finally {
            profilesLoading = false;
        }
    }

    onMount(async () => {
        applyIngestionSourceTypePreset(ingestionForm.sourceType);
        applyMappingSourceType(mappingForm.sourceType);
        await Promise.all([loadRuns(), loadProfiles()]);
    });

    async function ingestAudience() {
        isBusy = true;
        try {
            const sourceConfig = advancedIngestionJson
                ? tryParseJson(ingestionForm.sourceConfigText, "sourceConfig")
                : ingestionForm.sourceType === "JSON"
                    ? buildJsonSourceConfigFromBuilder()
                    : rowsToObject(sourceConfigRows);
            const payload: Record<string, unknown> = {
                tenantId,
                sourceType: ingestionForm.sourceType,
                sourceConfig,
                dryRun: ingestionForm.dryRun,
            };
            if (ingestionForm.mappingProfileId.trim()) {
                payload.mappingProfileId = Number(ingestionForm.mappingProfileId);
            }
            const res = await api.post("/audience/ingest", payload);
            lastIngestionResponse = res.data;
            toast.success(`Ingestion run created: ${res.data.runId}`);
            await loadRuns();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Ingestion failed");
        } finally {
            isBusy = false;
        }
    }

    async function fetchRunDetails(runId: string) {
        isBusy = true;
        try {
            const [runRes, rejRes] = await Promise.all([
                api.get(`/audience/ingestion-runs/${runId}`),
                api.get(`/audience/ingestion-runs/${runId}/rejections`, {
                    params: { limit: rejectionLimit },
                }),
            ]);
            selectedRun = runRes.data;
            runRejections = rejRes.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to fetch run details");
        } finally {
            isBusy = false;
        }
    }

    async function replayRun(runId: string, dryRun: boolean) {
        isBusy = true;
        try {
            const res = await api.post(`/audience/ingestion-runs/${runId}/replay`, { dryRun });
            lastIngestionResponse = res.data;
            toast.success(`Replay started: ${res.data.runId}`);
            await loadRuns();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Replay failed");
        } finally {
            isBusy = false;
        }
    }

    async function validateMapping() {
        isBusy = true;
        try {
            const res = await api.post("/audience/mapping-profiles/validate", {
                sourceType: mappingForm.sourceType,
                fieldMappings: advancedMappingsJson
                    ? tryParseJson(mappingForm.fieldMappingsText, "fieldMappings")
                    : rowsToMappings(mappingRows),
            });
            lastValidationResponse = res.data;
            mappingValidationText = JSON.stringify(res.data, null, 2);
            toast.success("Mapping validated");
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Validation failed");
        } finally {
            isBusy = false;
        }
    }

    async function createOrUpdateProfile() {
        isBusy = true;
        try {
            const payload = {
                tenantId,
                name: mappingForm.name,
                sourceType: mappingForm.sourceType,
                fieldMappings: advancedMappingsJson
                    ? tryParseJson(mappingForm.fieldMappingsText, "fieldMappings")
                    : rowsToMappings(mappingRows),
                active: mappingForm.active,
            };

            if (mappingForm.profileId.trim()) {
                await api.put(`/audience/mapping-profiles/${mappingForm.profileId}`, payload);
                toast.success("Mapping profile updated");
            } else {
                await api.post("/audience/mapping-profiles", payload);
                toast.success("Mapping profile created");
            }
            await loadProfiles();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Failed to save mapping profile");
        } finally {
            isBusy = false;
        }
    }

    async function deactivateProfile(profileId: string) {
        isBusy = true;
        try {
            await api.post(`/audience/mapping-profiles/${profileId}/deactivate`, {
                tenantId,
            });
            toast.success("Profile deactivated");
            await loadProfiles();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to deactivate profile");
        } finally {
            isBusy = false;
        }
    }

    async function loadProfileEvents(profileId: string) {
        isBusy = true;
        selectedProfileId = profileId;
        try {
            const res = await api.get(`/audience/mapping-profiles/${profileId}/events`, {
                params: { tenantId, limit: profileEventLimit },
            });
            profileEvents = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load profile events");
        } finally {
            isBusy = false;
        }
    }

    function loadProfileIntoForm(profile: any) {
        mappingForm.profileId = String(profile.id);
        mappingForm.name = profile.name;
        mappingForm.sourceType = normalizeAudienceSourceType(profile.sourceType);
        mappingForm.active = profile.active;
        mappingForm.fieldMappingsText = JSON.stringify(profile.fieldMappings || {}, null, 2);
        const entries = Object.entries(profile.fieldMappings || {});
        mappingRows = entries.length
            ? entries.map(([source, target]) => ({
                  id: crypto.randomUUID(),
                  source: String(source),
                  target: String(target),
              }))
            : [{ id: crypto.randomUUID(), source: "", target: "" }];
    }

    function resetProfileForm() {
        mappingForm.profileId = "";
        mappingForm.name = "";
        mappingForm.sourceType = "JSON";
        mappingForm.active = true;
        mappingForm.fieldMappingsText = '{"person_id":"id","display_name":"name","email":"email"}';
        mappingRows = [
            { id: crypto.randomUUID(), source: "person_id", target: "id" },
            { id: crypto.randomUUID(), source: "display_name", target: "name" },
            { id: crypto.randomUUID(), source: "email", target: "email" },
        ];
    }
</script>

<div class="space-y-6 pb-20">
    <div class="flex items-center justify-between gap-4">
        <div>
            <h1 class="text-3xl font-bold tracking-tight">Audience</h1>
            <p class="text-muted-foreground">
                Ingestion runs, mapping profiles, replay, and validation.
            </p>
        </div>
        <div class="w-[260px]">
            <div class="flex items-center gap-1">
                <Label for="tenant-id">Tenant ID</Label>
                <HelpInfo
                    title="Tenant ID"
                    what="Identifies the tenant partition used for audience/rule data."
                    why="All audience and mapping APIs are tenant-scoped."
                    how="Use an existing tenant ID; in dev default is tenant-001."
                    example="tenant-001"
                />
            </div>
            <Input id="tenant-id" bind:value={tenantId} placeholder="tenant-001" />
        </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Ingest Audience</Card.Title>
                    <HelpInfo
                        title="Ingest Audience Section"
                        what="Creates ingestion runs that load audience data into canonical tables."
                        why="Needed before mapping/rule simulation has usable people/groups/memberships."
                        how="Set source type + source config; optionally mapping profile; run dry first."
                        example='sourceType=JSON, sourceConfig=entityType:PERSON'
                    />
                </div>
                <Card.Description>POST /api/v1/audience/ingest</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Source Type</Label>
                        <HelpInfo
                            title="Source Type"
                            what="Connector type for ingestion."
                            why="Backend uses this to resolve parsing/connector strategy."
                            how="Use JSON/CSV/REST/JDBC depending on your source."
                            example="JSON"
                        />
                    </div>
                    <select
                        bind:value={ingestionForm.sourceType}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        onchange={(e) => applyIngestionSourceTypePreset((e.currentTarget as HTMLSelectElement).value)}
                    >
                        {#each AUDIENCE_SOURCE_TYPES as type}
                            <option value={type}>{type}</option>
                        {/each}
                    </select>
                    <p class="text-xs text-muted-foreground">{sourceTypeRequiredText(ingestionForm.sourceType)}</p>
                    <p class="text-xs text-muted-foreground">Example: {sourceTypeExampleText(ingestionForm.sourceType)}</p>
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Mapping Profile (optional)</Label>
                        <HelpInfo
                            title="Mapping Profile"
                            what="Existing mapping profile to normalize incoming fields."
                            why="Prevents manual field alignment in each ingest."
                            how="Leave empty to ingest without profile; otherwise pick a profile by name."
                            example="HR JSON v1"
                        />
                    </div>
                    <select
                        bind:value={ingestionForm.mappingProfileId}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        <option value="">No profile</option>
                        {#each profiles as profile}
                            <option value={String(profile.id)}>{profile.name}</option>
                        {/each}
                    </select>
                </div>
                <div class="flex items-center gap-2">
                    <Checkbox checked={ingestionForm.dryRun} onCheckedChange={(v) => (ingestionForm.dryRun = v)} />
                    <Label>Dry Run</Label>
                    <HelpInfo
                        title="Dry Run"
                        what="Validates and processes payload without persisting final records."
                        why="Safe validation path before real ingestion."
                        how="Enable for first execution; disable for final load."
                        example="true"
                    />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Source Config</Label>
                        <HelpInfo
                            title="Source Config"
                            what="Connector-specific options passed to ingestion service."
                            why="Controls entity type, record payload, and connector options."
                            how="Use form rows for normal use; switch advanced mode for complex JSON."
                            example="entityType: PERSON, records: [person_id:u1, display_name:Alice]"
                        />
                    </div>
                    <div class="flex items-center justify-between rounded border px-3 py-2">
                        <span class="text-sm">Advanced JSON Mode</span>
                        <Switch bind:checked={advancedIngestionJson} />
                    </div>
                    {#if advancedIngestionJson}
                        <Textarea rows={8} bind:value={ingestionForm.sourceConfigText} />
                    {:else}
                        <div class="space-y-2 rounded-md border p-3">
                            {#if ingestionForm.sourceType === "JSON"}
                                <div class="grid gap-2">
                                    <div class="flex items-center gap-1">
                                        <Label>Entity Type</Label>
                                        <HelpInfo
                                            title="JSON Entity Type"
                                            what="Target canonical entity kind."
                                            why="Determines validation and persistence model."
                                            how="Choose PERSON, GROUP, or MEMBERSHIP."
                                            example="PERSON"
                                        />
                                    </div>
                                    <select
                                        bind:value={jsonEntityType}
                                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                                        onchange={(e) => updateJsonEntityType((e.currentTarget as HTMLSelectElement).value)}
                                    >
                                        <option value="PERSON">PERSON</option>
                                        <option value="GROUP">GROUP</option>
                                        <option value="MEMBERSHIP">MEMBERSHIP</option>
                                    </select>
                                </div>
                                <div class="space-y-2">
                                    <div class="flex items-center gap-1">
                                        <Label>Records</Label>
                                        <HelpInfo
                                            title="JSON Records"
                                            what="Row-based record builder for JSON ingestion."
                                            why="Avoids manual JSON typing for normal admin use."
                                            how="Add rows and fill fields; empty fields are omitted."
                                            example="person_id=u1, display_name=Alice, email=alice@example.com"
                                        />
                                    </div>
                                    <div class="overflow-auto rounded-md border">
                                        <table class="min-w-full text-sm">
                                            <thead class="bg-muted/30">
                                                <tr>
                                                    {#each jsonFieldKeys(jsonEntityType) as key}
                                                        <th class="px-2 py-2 text-left font-medium">{key}</th>
                                                    {/each}
                                                    <th class="px-2 py-2 text-left font-medium">Action</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {#each jsonRecordRows as row}
                                                    <tr class="border-t">
                                                        {#each jsonFieldKeys(jsonEntityType) as key}
                                                            <td class="px-2 py-2">
                                                                <Input
                                                                    value={row.fields[key] ?? ""}
                                                                    placeholder={key}
                                                                    oninput={(e) =>
                                                                        (row.fields[key] = (e.currentTarget as HTMLInputElement).value)}
                                                                />
                                                            </td>
                                                        {/each}
                                                        <td class="px-2 py-2">
                                                            <Button type="button" variant="ghost" size="icon" onclick={() => removeJsonRecordRow(row.id)}>
                                                                <Trash2 class="h-4 w-4" />
                                                            </Button>
                                                        </td>
                                                    </tr>
                                                {/each}
                                            </tbody>
                                        </table>
                                    </div>
                                    <Button type="button" variant="outline" size="sm" onclick={addJsonRecordRow}>
                                        <Plus class="mr-2 h-4 w-4" /> Add Record
                                    </Button>
                                </div>
                            {:else}
                                <p class="text-xs text-muted-foreground">
                                    Add source config as key/value pairs (numbers and booleans auto-typed).
                                </p>
                                <div class="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                                    <div class="flex items-center gap-1"><span>Key</span><HelpInfo title="Config Key" what="Configuration property name." why="Backend reads values by key." how="Use exact expected key." example="url" /></div>
                                    <div class="flex items-center gap-1"><span>Value</span><HelpInfo title="Config Value" what="Configuration property value." why="Determines runtime behavior." how="Use scalar text/number/bool; JSON for objects/arrays." example="https://api.example.com/people" /></div>
                                </div>
                                {#each sourceConfigRows as row}
                                    <div class="grid grid-cols-[1fr_1fr_auto] gap-2">
                                        <Input bind:value={row.key} placeholder="key" />
                                        <Input bind:value={row.value} placeholder="value" />
                                        <Button type="button" variant="ghost" size="icon" onclick={() => removeSourceConfigRow(row.id)}>
                                            <Trash2 class="h-4 w-4" />
                                        </Button>
                                    </div>
                                {/each}
                                <Button type="button" variant="outline" size="sm" onclick={addSourceConfigRow}>
                                    <Plus class="mr-2 h-4 w-4" /> Add Row
                                </Button>
                            {/if}
                        </div>
                    {/if}
                </div>
                {#if lastIngestionResponse}
                    <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto">{JSON.stringify(lastIngestionResponse, null, 2)}</pre>
                {/if}
            </Card.Content>
            <Card.Footer>
                <Button onclick={ingestAudience} disabled={isBusy}>
                    {#if isBusy}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                    {/if}
                    Start Ingestion
                </Button>
            </Card.Footer>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Mapping Profile</Card.Title>
                    <HelpInfo
                        title="Mapping Profile Section"
                        what="Defines reusable source->canonical field mappings."
                        why="Keeps ingestion payloads consistent across runs."
                        how="Create profile once, validate, then reuse by profile ID in ingestion."
                        example="person_id->id, display_name->name, email->email"
                    />
                </div>
                <Card.Description>Create, update, validate, deactivate</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Name</Label>
                        <HelpInfo title="Profile Name" what="Human-readable profile identifier." why="Helps admins pick correct mapping." how="Use descriptive source+version naming." example="HR JSON v1" />
                    </div>
                    <Input bind:value={mappingForm.name} placeholder="Default JSON mapping" />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Source Type</Label>
                        <HelpInfo title="Mapping Source Type" what="Connector this mapping applies to." why="Mappings may differ per source format." how="Match it with ingestion source type." example="JSON" />
                    </div>
                    <select
                        bind:value={mappingForm.sourceType}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        onchange={(e) => applyMappingSourceType((e.currentTarget as HTMLSelectElement).value)}
                    >
                        {#each AUDIENCE_SOURCE_TYPES as type}
                            <option value={type}>{type}</option>
                        {/each}
                    </select>
                </div>
                <div class="flex items-center gap-2">
                    <Checkbox checked={mappingForm.active} onCheckedChange={(v) => (mappingForm.active = v)} />
                    <Label>Active</Label>
                    <HelpInfo title="Profile Active Flag" what="Whether profile is selectable for ingestion." why="Allows safe deprecation without delete." how="Set true for current profile, false to retire." example="true" />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Field Mappings</Label>
                        <HelpInfo
                            title="Field Mappings"
                            what="Map incoming source keys to canonical target keys."
                            why="Canonical schema expects stable target names."
                            how="Use left=source, right=canonical field."
                            example='source: person_id -> target: id'
                        />
                    </div>
                    <div class="flex items-center justify-between rounded border px-3 py-2">
                        <span class="text-sm">Advanced JSON Mode</span>
                        <Switch bind:checked={advancedMappingsJson} />
                    </div>
                    {#if advancedMappingsJson}
                        <Textarea rows={8} bind:value={mappingForm.fieldMappingsText} />
                    {:else}
                        <div class="space-y-2 rounded-md border p-3">
                            <p class="text-xs text-muted-foreground">
                                Define mapping as source field -> canonical field.
                            </p>
                            <div class="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                                <div class="flex items-center gap-1"><span>Source Field</span><HelpInfo title="Source Field" what="Raw field in incoming data." why="Needed to locate source value." how="Use exact incoming key." example="display_name" /></div>
                                <div class="flex items-center gap-1"><span>Target Field</span><HelpInfo title="Target Field" what="Canonical field name expected by backend." why="Ensures normalized schema." how="Pick supported canonical key." example="name" /></div>
                            </div>
                            {#each mappingRows as row}
                                <div class="grid grid-cols-[1fr_1fr_auto] gap-2">
                                    <Input bind:value={row.source} placeholder="source field" />
                                    <Input bind:value={row.target} placeholder="target field" />
                                    <Button type="button" variant="ghost" size="icon" onclick={() => removeMappingRow(row.id)}>
                                        <Trash2 class="h-4 w-4" />
                                    </Button>
                                </div>
                            {/each}
                            <Button type="button" variant="outline" size="sm" onclick={addMappingRow}>
                                <Plus class="mr-2 h-4 w-4" /> Add Mapping
                            </Button>
                        </div>
                    {/if}
                </div>
                {#if mappingValidationText}
                    <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto">{mappingValidationText}</pre>
                {/if}
            </Card.Content>
            <Card.Footer class="flex gap-2">
                <Button onclick={createOrUpdateProfile} disabled={isBusy}>
                    {mappingForm.profileId ? "Update" : "Create"} Profile
                </Button>
                <Button variant="outline" onclick={validateMapping} disabled={isBusy}>Validate</Button>
                <Button variant="ghost" onclick={resetProfileForm} disabled={isBusy}>Reset</Button>
            </Card.Footer>
        </Card.Root>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
        <Card.Root>
            <Card.Header class="flex flex-row items-center justify-between">
                <div>
                    <div class="flex items-center gap-1">
                        <Card.Title>Ingestion Runs</Card.Title>
                        <HelpInfo title="Ingestion Runs" what="History of ingestion attempts." why="Operational visibility and replay entry point." how="Open details, inspect rejections, replay if needed." example="runId=3c... status=FAILED" />
                    </div>
                    <Card.Description>List and replay runs</Card.Description>
                </div>
                <Button variant="outline" size="sm" onclick={loadRuns} disabled={runsLoading}>
                    <RefreshCcw class="h-4 w-4" />
                </Button>
            </Card.Header>
            <Card.Content class="space-y-2">
                {#if runsLoading}
                    <p class="text-sm text-muted-foreground">Loading runs...</p>
                {:else if runs.length === 0}
                    <p class="text-sm text-muted-foreground">No runs found.</p>
                {:else}
                    {#each runs as run}
                        <div class="rounded border p-3 space-y-2">
                            <div class="flex items-center justify-between gap-2">
                                <div class="text-sm font-medium">{run.id}</div>
                                <div class="text-xs text-muted-foreground">{run.status}</div>
                            </div>
                            <div class="text-xs text-muted-foreground">
                                {run.sourceType} | processed {run.processedRecords} | rejected {run.rejectedRecords}
                            </div>
                            <div class="flex gap-2">
                                <Button size="sm" variant="outline" onclick={() => fetchRunDetails(run.id)} disabled={isBusy}>Details</Button>
                                <Button size="sm" variant="outline" onclick={() => replayRun(run.id, true)} disabled={isBusy}>Replay Dry</Button>
                                <Button size="sm" onclick={() => replayRun(run.id, false)} disabled={isBusy}>Replay Live</Button>
                            </div>
                        </div>
                    {/each}
                {/if}
            </Card.Content>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Run Details / Rejections</Card.Title>
                    <HelpInfo title="Run Details" what="Full run metadata and rejected row diagnostics." why="Required for data-quality triage." how="Select a run from list and tune rejection limit." example="reason=Missing person_id" />
                </div>
                <Card.Description>GET run + rejections</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Rejection Limit</Label>
                        <HelpInfo title="Rejection Limit" what="Max rejected rows returned per run." why="Avoid overloading UI for large failures." how="Start 200, increase for deeper analysis." example="200" />
                    </div>
                    <Input type="number" min="1" bind:value={rejectionLimit} />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Selected Run</Label>
                        <HelpInfo title="Selected Run" what="Choose run to load details/rejections." why="Avoids copying run IDs manually." how="Pick from list and click Load Selected Run." example="FAILED | JSON | 2026-02-19" />
                    </div>
                    <select
                        bind:value={selectedRunId}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        {#each runs as run}
                            <option value={run.id}
                                >{run.status} | {run.sourceType} | {new Date(run.startedAt).toLocaleString()}</option
                            >
                        {/each}
                    </select>
                    <Button
                        variant="outline"
                        onclick={() => selectedRunId && fetchRunDetails(selectedRunId)}
                        disabled={!selectedRunId || isBusy}
                        >Load Selected Run</Button
                    >
                </div>
                {#if selectedRun}
                    <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto">{JSON.stringify(selectedRun, null, 2)}</pre>
                {/if}
                {#if runRejections.length > 0}
                    <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto">{JSON.stringify(runRejections, null, 2)}</pre>
                {/if}
            </Card.Content>
        </Card.Root>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
        <Card.Root>
            <Card.Header class="flex flex-row items-center justify-between">
                <div>
                    <div class="flex items-center gap-1">
                        <Card.Title>Mapping Profiles</Card.Title>
                        <HelpInfo title="Mapping Profiles List" what="Tenant mapping profiles with status." why="Manage lifecycle and quickly load profile for edit." how="Use Edit/Events/Deactivate actions per row." example="HR JSON v1" />
                    </div>
                    <Card.Description>GET/list + load/deactivate</Card.Description>
                </div>
                <Button variant="outline" size="sm" onclick={loadProfiles} disabled={profilesLoading}>
                    <RefreshCcw class="h-4 w-4" />
                </Button>
            </Card.Header>
            <Card.Content class="space-y-2">
                {#if profilesLoading}
                    <p class="text-sm text-muted-foreground">Loading profiles...</p>
                {:else if profiles.length === 0}
                    <p class="text-sm text-muted-foreground">No profiles found.</p>
                {:else}
                    {#each profiles as profile}
                        <div class="rounded border p-3 space-y-2">
                            <div class="flex items-center justify-between">
                                <div class="text-sm font-medium">{profile.name}</div>
                                <div class="text-xs text-muted-foreground">{profile.active ? "ACTIVE" : "INACTIVE"}</div>
                            </div>
                            <div class="text-xs text-muted-foreground">{profile.sourceType}</div>
                            <div class="flex gap-2">
                                <Button size="sm" variant="outline" onclick={() => loadProfileIntoForm(profile)}>Edit</Button>
                                <Button size="sm" variant="outline" onclick={() => loadProfileEvents(String(profile.id))}>Events</Button>
                                <Button size="sm" variant="destructive" onclick={() => deactivateProfile(String(profile.id))} disabled={!profile.active}>Deactivate</Button>
                            </div>
                        </div>
                    {/each}
                {/if}
            </Card.Content>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Profile Events</Card.Title>
                    <HelpInfo title="Profile Events" what="Audit-style lifecycle events for a mapping profile." why="Tracks who changed what and when." how="Provide profile ID and limit, then load events." example="eventType=UPDATED actor=admin" />
                </div>
                <Card.Description>GET /mapping-profiles/{'{id}'}/events</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Profile</Label>
                        <HelpInfo title="Profile" what="Mapping profile whose events you want to inspect." why="Event API needs an explicit profile target." how="Choose profile by name from the list." example="HR JSON v1" />
                    </div>
                    <select
                        bind:value={selectedProfileId}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        {#each profiles as profile}
                            <option value={String(profile.id)}>{profile.name}</option>
                        {/each}
                    </select>
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Limit</Label>
                        <HelpInfo title="Events Limit" what="Max number of events returned." why="Controls payload size." how="Use 50 by default and increase only when needed." example="50" />
                    </div>
                    <Input type="number" min="1" bind:value={profileEventLimit} />
                </div>
                <Button variant="outline" onclick={() => selectedProfileId && loadProfileEvents(selectedProfileId)} disabled={!selectedProfileId || isBusy}>Load Events</Button>
                {#if profileEvents.length > 0}
                    <pre class="rounded-md border bg-muted/30 p-3 text-xs overflow-auto">{JSON.stringify(profileEvents, null, 2)}</pre>
                {/if}
                {#if lastValidationResponse}
                    <p class="text-xs text-muted-foreground">Last mapping validation is shown above.</p>
                {/if}
            </Card.Content>
        </Card.Root>
    </div>
</div>
