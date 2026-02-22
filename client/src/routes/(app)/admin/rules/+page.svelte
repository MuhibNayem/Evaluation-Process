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
    import DataView from "$lib/components/data-view.svelte";
    import { toast } from "svelte-sonner";

    let tenantId = $state(DEFAULT_TENANT_ID);
    let statusFilter = $state("");

    let capabilities: any = $state(null);
    let rules = $state<any[]>([]);
    let campaigns = $state<any[]>([]);
    let publishRequests = $state<any[]>([]);

    let ruleForm = $state({
        id: "",
        name: "",
        description: "",
        semanticVersion: "1.0.0",
        ruleType: "ATTRIBUTE_MATCH",
        ruleConfigText: '{"matchKey":"department"}',
    });

    let publishForm = $state({
        ruleId: "",
        reasonCode: "",
        comment: "",
        publishRequestId: "",
        decisionComment: "",
    });

    let runForm = $state({
        ruleId: "",
        campaignId: "",
        audienceSourceType: "INLINE",
        audienceSourceConfigText: '{"participants":[]}',
        replaceExistingAssignments: false,
        dryRun: true,
        diagnosticMode: true,
    });

    let lastResponse = $state<any>(null);
    let isBusy = $state(false);
    let rulesLoading = $state(false);
    let campaignsLoading = $state(false);
    let advancedRuleConfigJson = $state(false);
    let advancedAudienceConfigJson = $state(false);
    let advancedRuleTypeOverride = $state(false);
    let ruleConfigRows = $state<{ id: string; key: string; value: string }[]>([
        { id: crypto.randomUUID(), key: "matchKey", value: "department" },
    ]);
    let audienceConfigRows = $state<{ id: string; key: string; value: string }[]>([
        { id: crypto.randomUUID(), key: "participants", value: "[]" },
    ]);

    function tryParseJson(input: string, fieldName: string) {
        try {
            return JSON.parse(input || "{}");
        } catch {
            throw new Error(`${fieldName} must be valid JSON`);
        }
    }

    function parseScalar(value: string): string | number | boolean {
        const trimmed = value.trim();
        if (trimmed.toLowerCase() === "true") return true;
        if (trimmed.toLowerCase() === "false") return false;
        if (/^-?\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
        return value;
    }

    function rowsToObject(rows: { key: string; value: string }[]) {
        const out: Record<string, unknown> = {};
        for (const row of rows) {
            if (!row.key.trim()) continue;
            const raw = row.value.trim();
            if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
                try {
                    out[row.key.trim()] = JSON.parse(raw);
                    continue;
                } catch {
                    // Fallback to scalar/string when not valid JSON
                }
            }
            out[row.key.trim()] = parseScalar(row.value);
        }
        return out;
    }

    function objectToRows(obj: Record<string, unknown>) {
        const entries = Object.entries(obj || {});
        if (!entries.length) {
            return [{ id: crypto.randomUUID(), key: "", value: "" }];
        }
        return entries.map(([key, value]) => ({
            id: crypto.randomUUID(),
            key,
            value:
                typeof value === "string" || typeof value === "number" || typeof value === "boolean"
                    ? String(value)
                    : JSON.stringify(value),
        }));
    }

    function addRuleConfigRow() {
        ruleConfigRows = [...ruleConfigRows, { id: crypto.randomUUID(), key: "", value: "" }];
    }

    function removeRuleConfigRow(id: string) {
        ruleConfigRows = ruleConfigRows.filter((r) => r.id !== id);
    }

    function addAudienceConfigRow() {
        audienceConfigRows = [...audienceConfigRows, { id: crypto.randomUUID(), key: "", value: "" }];
    }

    function removeAudienceConfigRow(id: string) {
        audienceConfigRows = audienceConfigRows.filter((r) => r.id !== id);
    }

    function defaultRuleConfigRows(ruleType: string) {
        switch ((ruleType || "").toUpperCase()) {
            case "ALL_TO_ALL":
                return [{ id: crypto.randomUUID(), key: "excludeSelf", value: "true" }];
            case "ROUND_ROBIN":
                return [
                    { id: crypto.randomUUID(), key: "groupSize", value: "1" },
                    { id: crypto.randomUUID(), key: "excludeSelf", value: "true" },
                ];
            case "MANAGER_HIERARCHY":
                return [
                    { id: crypto.randomUUID(), key: "managerAttributeKey", value: "managerId" },
                    { id: crypto.randomUUID(), key: "excludeSelf", value: "true" },
                ];
            case "ATTRIBUTE_MATCH":
            default:
                return [
                    { id: crypto.randomUUID(), key: "matchKey", value: "department" },
                    { id: crypto.randomUUID(), key: "excludeSelf", value: "true" },
                ];
        }
    }

    function ruleTypeCapabilityHelp(ruleType: string) {
        switch ((ruleType || "").toUpperCase()) {
            case "ALL_TO_ALL":
                return {
                    what: "Generates evaluator/evaluatee pairs across the full participant set.",
                    why: "Useful for broad peer-style coverage where everyone can evaluate many users.",
                    how: "Configure controls like self-evaluation and max evaluators per evaluatee.",
                    example: "allowSelfEvaluation=false, maxEvaluatorsPerEvaluatee=3",
                };
            case "ROUND_ROBIN":
                return {
                    what: "Distributes assignments in rotating order across participants.",
                    why: "Improves load balancing and avoids concentration on a small subset.",
                    how: "Set evaluatorsPerEvaluatee and self-evaluation policy.",
                    example: "evaluatorsPerEvaluatee=1, allowSelfEvaluation=false",
                };
            case "MANAGER_HIERARCHY":
                return {
                    what: "Assigns manager/supervisor as evaluator for each participant.",
                    why: "Supports reporting-line based evaluation programs.",
                    how: "Ensure participants include managerId/supervisorId and set strictness flags.",
                    example: "requireKnownManager=true, includeSelfEvaluation=false",
                };
            case "ATTRIBUTE_MATCH":
            default:
                return {
                    what: "Matches evaluator/evaluatee by a shared attribute value.",
                    why: "Supports section/department/team scoped matching.",
                    how: "Set matchAttribute and optional caps.",
                    example: "matchAttribute=department, maxEvaluatorsPerEvaluatee=3",
                };
        }
    }

    function audienceTypeCapabilityHelp(sourceType: string) {
        switch ((sourceType || "").toUpperCase()) {
            case "DIRECTORY_SNAPSHOT":
                return {
                    what: "Uses snapshot-style participant payload with participants array.",
                    why: "Useful when audience comes from a directory export snapshot.",
                    how: "Provide audienceSourceConfig.participants as array of objects.",
                    example: 'participants=[{"userId":"u1","department":"CSE"}]',
                };
            case "INLINE":
            default:
                return {
                    what: "Uses inline participant payload provided directly in request config.",
                    why: "Fast for simulation/testing or custom prepared audience input.",
                    how: "Provide audienceSourceConfig.participants as array of objects.",
                    example: 'participants=[{"userId":"u1","managerId":"m1"}]',
                };
        }
    }

    function applyRuleTypePreset(nextRuleType: string) {
        ruleForm.ruleType = nextRuleType;
        if (!advancedRuleConfigJson) {
            ruleConfigRows = defaultRuleConfigRows(nextRuleType);
        }
    }

    async function loadCapabilities() {
        try {
            const res = await api.get("/admin/rules/capabilities");
            capabilities = res.data;
            if (!ruleForm.ruleType && capabilities?.supportedRuleTypes?.length) {
                ruleForm.ruleType = capabilities.supportedRuleTypes[0];
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load capabilities");
        }
    }

    async function loadRules() {
        rulesLoading = true;
        try {
            const res = await api.get("/admin/rules", {
                params: {
                    tenantId,
                    ...(statusFilter.trim() ? { status: statusFilter.trim().toUpperCase() } : {}),
                },
            });
            rules = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load rules");
        } finally {
            rulesLoading = false;
        }
    }

    async function loadCampaigns() {
        campaignsLoading = true;
        try {
            const res = await api.get("/campaigns");
            campaigns = res.data;
            if (!runForm.campaignId && campaigns.length > 0) {
                runForm.campaignId = campaigns[0].id;
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load campaigns");
        } finally {
            campaignsLoading = false;
        }
    }

    onMount(async () => {
        await Promise.all([loadCapabilities(), loadRules(), loadCampaigns()]);
    });

    async function createOrUpdateRule() {
        isBusy = true;
        try {
            const payload = {
                tenantId,
                name: ruleForm.name,
                description: ruleForm.description,
                semanticVersion: ruleForm.semanticVersion,
                ruleType: ruleForm.ruleType,
                ruleConfig: advancedRuleConfigJson
                    ? tryParseJson(ruleForm.ruleConfigText, "ruleConfig")
                    : rowsToObject(ruleConfigRows),
            };

            const res = ruleForm.id
                ? await api.put(`/admin/rules/${ruleForm.id}`, payload)
                : await api.post("/admin/rules", payload);
            lastResponse = res.data;
            toast.success(ruleForm.id ? "Rule updated" : "Rule created");
            if (!ruleForm.id) {
                loadRuleToForm(res.data);
            }
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Failed to save rule");
        } finally {
            isBusy = false;
        }
    }

    async function createPublishRequest() {
        if (!publishForm.ruleId) return;
        isBusy = true;
        try {
            const res = await api.post(`/admin/rules/${publishForm.ruleId}/publish-requests`, {
                tenantId,
                reasonCode: publishForm.reasonCode,
                comment: publishForm.comment,
            });
            lastResponse = res.data;
            publishForm.publishRequestId = String(res.data.id);
            publishRequests = [res.data, ...publishRequests.filter((p) => p.id !== res.data.id)];
            toast.success("Publish request created");
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to create publish request");
        } finally {
            isBusy = false;
        }
    }

    async function decidePublishRequest(action: "approve" | "reject") {
        if (!publishForm.publishRequestId) return;
        isBusy = true;
        try {
            const res = await api.post(
                `/admin/rules/publish-requests/${publishForm.publishRequestId}/${action}`,
                {
                    tenantId,
                    decisionComment: publishForm.decisionComment,
                },
            );
            lastResponse = res.data;
            publishRequests = [res.data, ...publishRequests.filter((p) => p.id !== res.data.id)];
            toast.success(`Publish request ${action}d`);
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || `Failed to ${action} publish request`);
        } finally {
            isBusy = false;
        }
    }

    async function deprecateRule(ruleId: string) {
        isBusy = true;
        try {
            const res = await api.post(`/admin/rules/${ruleId}/deprecate`, {
                tenantId,
                decisionComment: publishForm.decisionComment,
            });
            lastResponse = res.data;
            toast.success("Rule deprecated");
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to deprecate rule");
        } finally {
            isBusy = false;
        }
    }

    async function simulateRule() {
        if (!runForm.ruleId) return;
        isBusy = true;
        try {
            const res = await api.post(`/admin/rules/${runForm.ruleId}/simulate`, {
                tenantId,
                audienceSourceType: runForm.audienceSourceType,
                audienceSourceConfig: advancedAudienceConfigJson
                    ? tryParseJson(runForm.audienceSourceConfigText, "audienceSourceConfig")
                    : rowsToObject(audienceConfigRows),
                diagnosticMode: runForm.diagnosticMode,
            });
            lastResponse = res.data;
            toast.success("Simulation completed");
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Simulation failed");
        } finally {
            isBusy = false;
        }
    }

    async function publishAssignments() {
        if (!runForm.ruleId || !runForm.campaignId) return;
        isBusy = true;
        try {
            const res = await api.post(`/admin/rules/${runForm.ruleId}/publish-assignments`, {
                tenantId,
                campaignId: runForm.campaignId,
                audienceSourceType: runForm.audienceSourceType,
                audienceSourceConfig: advancedAudienceConfigJson
                    ? tryParseJson(runForm.audienceSourceConfigText, "audienceSourceConfig")
                    : rowsToObject(audienceConfigRows),
                replaceExistingAssignments: runForm.replaceExistingAssignments,
                dryRun: runForm.dryRun,
            });
            lastResponse = res.data;
            toast.success("Publish assignments completed");
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Publish assignments failed");
        } finally {
            isBusy = false;
        }
    }

    function loadRuleToForm(rule: any) {
        ruleForm.id = String(rule.id);
        ruleForm.name = rule.name;
        ruleForm.description = rule.description || "";
        ruleForm.semanticVersion = rule.semanticVersion;
        ruleForm.ruleType = rule.ruleType;
        ruleForm.ruleConfigText = JSON.stringify(rule.ruleConfig || {}, null, 2);
        ruleConfigRows = objectToRows(rule.ruleConfig || {});
        publishForm.ruleId = String(rule.id);
        runForm.ruleId = String(rule.id);
    }

    function clearRuleForm() {
        ruleForm.id = "";
        ruleForm.name = "";
        ruleForm.description = "";
        ruleForm.semanticVersion = "1.0.0";
        ruleForm.ruleType = capabilities?.supportedRuleTypes?.[0] || "ATTRIBUTE_MATCH";
        ruleForm.ruleConfigText = '{"matchKey":"department"}';
        ruleConfigRows = defaultRuleConfigRows(ruleForm.ruleType);
    }

    function ruleLabel(rule: any) {
        return `${rule.name} [${rule.status}]`;
    }

    function campaignLabel(campaign: any) {
        return `${campaign.name} [${campaign.status}]`;
    }

    function ruleNameById(ruleId: string | number) {
        const found = rules.find((r) => String(r.id) === String(ruleId));
        return found?.name || `Rule ${ruleId}`;
    }
</script>

<div class="space-y-6 pb-20">
    <div class="flex items-center justify-between gap-4">
        <div>
            <h1 class="text-3xl font-bold tracking-tight">Rule Control Plane</h1>
            <p class="text-muted-foreground">
                Rule definition lifecycle, publish workflow, simulation, and assignment publishing.
            </p>
        </div>
        <div class="w-[260px]">
            <div class="flex items-center gap-1">
                <Label>Tenant ID</Label>
                <HelpInfo
                    title="Tenant ID"
                    what="Tenant partition where rules are created and managed."
                    why="Rule definitions and publish requests are tenant-scoped."
                    how="Use an existing tenant ID from your environment."
                    example="tenant-001"
                />
            </div>
            <Input bind:value={tenantId} placeholder="tenant-001" />
        </div>
    </div>

    <Card.Root>
        <Card.Header class="flex flex-row items-center justify-between">
            <div>
                <div class="flex items-center gap-1">
                    <Card.Title>Capabilities</Card.Title>
                    <HelpInfo
                        title="Capabilities"
                        what="Backend-supported rule types, audience source types, and workflow flags."
                        why="Prevents invalid config choices."
                        how="Refresh capabilities before creating new rule types."
                        example='supportedRuleTypes=["ATTRIBUTE_MATCH","ROUND_ROBIN"]'
                    />
                </div>
                <Card.Description>GET /api/v1/admin/rules/capabilities</Card.Description>
            </div>
            <Button variant="outline" size="sm" onclick={loadCapabilities}><RefreshCcw class="h-4 w-4" /></Button>
        </Card.Header>
        <Card.Content class="space-y-4">
            {#if capabilities}
                <div class="grid gap-4 md:grid-cols-3">
                    <div class="rounded-md border p-3">
                        <div class="mb-2 flex items-center gap-1">
                            <p class="text-sm font-medium">Supported Rule Types</p>
                            <HelpInfo
                                title="Supported Rule Types"
                                what="Rule algorithms enabled by backend for this environment."
                                why="Only these values can be used when creating/updating rule definitions."
                                how="Pick from this list unless using temporary advanced override."
                                example='["ATTRIBUTE_MATCH","ROUND_ROBIN"]'
                            />
                        </div>
                        <div class="flex flex-wrap gap-2">
                            {#each capabilities.supportedRuleTypes || [] as type}
                                <div class="flex items-center gap-1">
                                    <span class="rounded-full border px-2 py-1 text-xs">{type}</span>
                                    <HelpInfo
                                        title={`Rule Type: ${type}`}
                                        what={ruleTypeCapabilityHelp(type).what}
                                        why={ruleTypeCapabilityHelp(type).why}
                                        how={ruleTypeCapabilityHelp(type).how}
                                        example={ruleTypeCapabilityHelp(type).example}
                                    />
                                </div>
                            {/each}
                        </div>
                    </div>
                    <div class="rounded-md border p-3">
                        <div class="mb-2 flex items-center gap-1">
                            <p class="text-sm font-medium">Supported Audience Sources</p>
                            <HelpInfo
                                title="Supported Audience Sources"
                                what="Audience input modes accepted by simulation/publish APIs."
                                why="Invalid source types are rejected by backend."
                                how="Use one of these values in Audience Source Type."
                                example='["INLINE","DIRECTORY_SNAPSHOT"]'
                            />
                        </div>
                        <div class="flex flex-wrap gap-2">
                            {#each capabilities.supportedAudienceSourceTypes || [] as source}
                                <div class="flex items-center gap-1">
                                    <span class="rounded-full border px-2 py-1 text-xs">{source}</span>
                                    <HelpInfo
                                        title={`Audience Source: ${source}`}
                                        what={audienceTypeCapabilityHelp(source).what}
                                        why={audienceTypeCapabilityHelp(source).why}
                                        how={audienceTypeCapabilityHelp(source).how}
                                        example={audienceTypeCapabilityHelp(source).example}
                                    />
                                </div>
                            {/each}
                        </div>
                    </div>
                    <div class="rounded-md border p-3 space-y-2">
                        <div class="flex items-center gap-1">
                            <p class="text-sm font-medium">Workflow Controls</p>
                            <HelpInfo
                                title="Workflow Controls"
                                what="Governance flags that change rule publish lifecycle behavior."
                                why="These controls enforce operational and audit policy."
                                how="Treat as environment policy; adjust in backend admin config."
                                example="publishLockEnabled=true, requireFourEyesApproval=true"
                            />
                        </div>
                        <div class="flex items-center justify-between text-sm">
                            <div class="flex items-center gap-1">
                                <span>Publish Lock</span>
                                <HelpInfo
                                    title="Publish Lock"
                                    what="If enabled, approved publish request can transition rule to PUBLISHED."
                                    why="Prevents accidental use of unapproved DRAFT rules."
                                    how="When enabled, follow request -> approve -> publish flow strictly."
                                    example="Enabled in production governance mode."
                                />
                            </div>
                            <span class="rounded-full border px-2 py-1 text-xs">
                                {capabilities.workflowConfig?.publishLockEnabled ? "Enabled" : "Disabled"}
                            </span>
                        </div>
                        <div class="flex items-center justify-between text-sm">
                            <div class="flex items-center gap-1">
                                <span>4-Eyes Approval</span>
                                <HelpInfo
                                    title="4-Eyes Approval"
                                    what="Requester cannot approve their own publish request."
                                    why="Ensures separation-of-duties for governance and compliance."
                                    how="A different admin user must approve/reject pending requests."
                                    example="Requester creates request, reviewer approves."
                                />
                            </div>
                            <span class="rounded-full border px-2 py-1 text-xs">
                                {capabilities.workflowConfig?.requireFourEyesApproval ? "Required" : "Optional"}
                            </span>
                        </div>
                    </div>
                </div>
                <div class="rounded-md border p-3">
                    <p class="text-sm font-medium mb-2">Capability Details</p>
                    <DataView data={capabilities} />
                </div>
            {:else}
                <p class="text-sm text-muted-foreground">No capability data yet.</p>
            {/if}
        </Card.Content>
    </Card.Root>

    <div class="grid gap-6 lg:grid-cols-2">
        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Rule Definition</Card.Title>
                    <HelpInfo
                        title="Rule Definition Section"
                        what="Create or update draft rule definitions."
                        why="Rules must exist before publish workflow/simulation."
                        how="Fill required fields and config, then create/update."
                        example="name=Peer Match, version=1.0.0, ruleType=ATTRIBUTE_MATCH"
                    />
                </div>
                <Card.Description>Create or update rule</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Rule To Edit (optional)</Label>
                        <HelpInfo title="Rule Selection" what="Select an existing rule to edit." why="Editing loads current values safely." how="Leave empty to create a new rule." example="Select Peer Match [DRAFT]" />
                    </div>
                    <select
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        value={ruleForm.id}
                        onchange={(e) => {
                            const id = (e.currentTarget as HTMLSelectElement).value;
                            if (!id) {
                                clearRuleForm();
                                return;
                            }
                            const selected = rules.find((r) => String(r.id) === id);
                            if (selected) loadRuleToForm(selected);
                        }}
                    >
                        <option value="">Create new rule</option>
                        {#each rules as rule}
                            <option value={String(rule.id)}>{ruleLabel(rule)}</option>
                        {/each}
                    </select>
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Name</Label>
                        <HelpInfo title="Rule Name" what="Human-readable rule name." why="Used to manage and audit rules." how="Use descriptive and stable naming." example="Peer Match by Department" />
                    </div>
                    <Input bind:value={ruleForm.name} placeholder="Peer Matching v1" />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Description</Label>
                        <HelpInfo title="Description" what="Free-text explanation of rule purpose." why="Helps reviewers approve/reject confidently." how="Capture intent and constraints." example="Assign peers from same department excluding self." />
                    </div>
                    <Textarea rows={3} bind:value={ruleForm.description} />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Semantic Version (x.y.z)</Label>
                        <HelpInfo title="Semantic Version" what="Rule version in semver format." why="Required for governance and traceability." how="Use MAJOR.MINOR.PATCH pattern only." example="1.2.0" />
                    </div>
                    <Input bind:value={ruleForm.semanticVersion} placeholder="1.0.0" />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Rule Type</Label>
                        <HelpInfo title="Rule Type" what="Assignment algorithm family." why="Determines how config is interpreted." how="Use value from capabilities supportedRuleTypes." example="ATTRIBUTE_MATCH" />
                    </div>
                    <div class="flex items-center justify-between rounded border px-3 py-2">
                        <span class="text-sm">Advanced Rule Type Override</span>
                        <Switch bind:checked={advancedRuleTypeOverride} />
                    </div>
                    {#if advancedRuleTypeOverride}
                        <Input bind:value={ruleForm.ruleType} placeholder="ATTRIBUTE_MATCH" />
                    {:else}
                        <select
                            bind:value={ruleForm.ruleType}
                            onchange={(e) =>
                                applyRuleTypePreset(
                                    (e.currentTarget as HTMLSelectElement).value,
                                )}
                            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        >
                            {#if capabilities?.supportedRuleTypes?.length}
                                {#each capabilities.supportedRuleTypes as type}
                                    <option value={type}>{type}</option>
                                {/each}
                            {:else}
                                <option value={ruleForm.ruleType}
                                    >{ruleForm.ruleType}</option
                                >
                            {/if}
                        </select>
                    {/if}
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Rule Config</Label>
                        <HelpInfo title="Rule Config" what="Parameters used by selected rule type." why="Without config, rule cannot be executed correctly." how="Use row editor for simple keys, JSON mode for complex objects." example="matchKey=department" />
                    </div>
                    <div class="flex items-center justify-between rounded border px-3 py-2">
                        <span class="text-sm">Advanced JSON Mode</span>
                        <Switch bind:checked={advancedRuleConfigJson} />
                    </div>
                    {#if advancedRuleConfigJson}
                        <Textarea rows={8} bind:value={ruleForm.ruleConfigText} />
                    {:else}
                        <div class="space-y-2 rounded-md border p-3">
                            <p class="text-xs text-muted-foreground">
                                Configure rule parameters as key/value rows.
                            </p>
                            <div class="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                                <div class="flex items-center gap-1"><span>Config Key</span><HelpInfo title="Rule Config Key" what="Parameter name for rule behavior." why="Backend reads rule config by key." how="Use documented key per rule type." example="matchKey" /></div>
                                <div class="flex items-center gap-1"><span>Config Value</span><HelpInfo title="Rule Config Value" what="Parameter value." why="Changes assignment outcome." how="Use scalar or JSON for advanced values." example="department" /></div>
                            </div>
                            {#each ruleConfigRows as row}
                                <div class="grid grid-cols-[1fr_1fr_auto] gap-2">
                                    <Input bind:value={row.key} placeholder="config key" />
                                    <Input bind:value={row.value} placeholder="value (or JSON)" />
                                    <Button type="button" variant="ghost" size="icon" onclick={() => removeRuleConfigRow(row.id)}>
                                        <Trash2 class="h-4 w-4" />
                                    </Button>
                                </div>
                            {/each}
                            <Button type="button" variant="outline" size="sm" onclick={addRuleConfigRow}>
                                <Plus class="mr-2 h-4 w-4" /> Add Rule Config Row
                            </Button>
                        </div>
                    {/if}
                </div>
            </Card.Content>
            <Card.Footer class="flex gap-2">
                <Button onclick={createOrUpdateRule} disabled={isBusy}>
                    {#if isBusy}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                    {ruleForm.id ? "Update" : "Create"} Rule
                </Button>
                <Button variant="outline" onclick={clearRuleForm}>Reset</Button>
            </Card.Footer>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <div class="flex items-center gap-1">
                    <Card.Title>Publish Workflow</Card.Title>
                    <HelpInfo
                        title="Publish Workflow Section"
                        what="Request publish, approve/reject, and deprecate rules."
                        why="Supports governed lifecycle with review controls."
                        how="Create publish request, then approve/reject by request ID."
                        example="POST /publish-requests -> approve by publishRequestId"
                    />
                </div>
                <Card.Description>Request publish, approve/reject, deprecate</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Rule</Label>
                        <HelpInfo title="Rule for Workflow" what="Target rule for publish/deprecate actions." why="Workflow actions are rule-specific." how="Choose from the list by name/status." example="Peer Match [DRAFT]" />
                    </div>
                    <select
                        bind:value={publishForm.ruleId}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        <option value="">Select rule</option>
                        {#each rules as rule}
                            <option value={String(rule.id)}>{ruleLabel(rule)}</option>
                        {/each}
                    </select>
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Reason Code</Label>
                        <HelpInfo title="Reason Code" what="Short classification for publish request intent." why="Improves audit/reporting consistency." how="Use your team taxonomy." example="CHANGE_REQUEST" />
                    </div>
                    <Input bind:value={publishForm.reasonCode} placeholder="CHANGE_REQUEST" />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Comment / Decision Comment</Label>
                        <HelpInfo title="Comment Fields" what="Human context for request/decision." why="Required for auditability and reviewer clarity." how="Explain why change is needed or approved/rejected." example="Needed for Q2 org restructure." />
                    </div>
                    <Textarea rows={3} bind:value={publishForm.comment} />
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Publish Request (approve/reject)</Label>
                        <HelpInfo title="Publish Request" what="Choose a pending request for approval/rejection." why="Decision actions operate on a specific request." how="Create request first, then select it here." example="Peer Match -> PENDING" />
                    </div>
                    <select
                        bind:value={publishForm.publishRequestId}
                        class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                        <option value="">Select publish request</option>
                        {#each publishRequests as request}
                            <option value={String(request.id)}
                                >{ruleNameById(request.ruleDefinitionId)} | {request.status} | request #{request.id}</option
                            >
                        {/each}
                    </select>
                </div>
                <div class="grid gap-2">
                    <div class="flex items-center gap-1">
                        <Label>Decision Comment</Label>
                        <HelpInfo title="Decision Comment" what="Reviewer rationale for approval/rejection/deprecation." why="Governance trail and postmortem context." how="Provide concise decision basis." example="Approved after test simulation and QA signoff." />
                    </div>
                    <Textarea rows={3} bind:value={publishForm.decisionComment} />
                </div>
            </Card.Content>
            <Card.Footer class="flex flex-wrap gap-2">
                <Button onclick={createPublishRequest} disabled={isBusy || !publishForm.ruleId}>Create Publish Request</Button>
                <Button variant="outline" onclick={() => decidePublishRequest("approve")} disabled={isBusy || !publishForm.publishRequestId}>Approve</Button>
                <Button variant="outline" onclick={() => decidePublishRequest("reject")} disabled={isBusy || !publishForm.publishRequestId}>Reject</Button>
                <Button variant="destructive" onclick={() => deprecateRule(publishForm.ruleId)} disabled={isBusy || !publishForm.ruleId}>Deprecate Rule</Button>
            </Card.Footer>
        </Card.Root>
    </div>

    <Card.Root>
        <Card.Header>
            <div class="flex items-center gap-1">
                <Card.Title>Simulate / Publish Assignments</Card.Title>
                <HelpInfo
                    title="Simulate / Publish Section"
                    what="Runs rule against audience and optionally writes assignments to campaign."
                    why="Validates outcomes before production publish."
                    how="Simulate first with diagnostic mode, then publish with dryRun=false."
                    example="simulate -> inspect generated/excluded -> publish"
                />
            </div>
            <Card.Description>Rule execution on audience config and campaign</Card.Description>
        </Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-2">
            <div class="grid gap-2">
                <div class="flex items-center gap-1">
                    <Label>Rule</Label>
                    <HelpInfo title="Rule to Run" what="Rule to simulate/publish." why="Execution needs explicit rule target." how="Choose by name/status." example="Peer Match [PUBLISHED]" />
                </div>
                <select
                    bind:value={runForm.ruleId}
                    class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                    <option value="">Select rule</option>
                    {#each rules as rule}
                        <option value={String(rule.id)}>{ruleLabel(rule)}</option>
                    {/each}
                </select>
            </div>
            <div class="grid gap-2">
                <div class="flex items-center gap-1">
                    <Label>Campaign (publish only)</Label>
                    <HelpInfo title="Campaign" what="Campaign receiving generated assignments." why="Publish endpoint writes into a specific campaign." how="Pick campaign by name/status." example="Spring 2026 [ACTIVE]" />
                </div>
                <select
                    bind:value={runForm.campaignId}
                    class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                    <option value="">Select campaign</option>
                    {#each campaigns as campaign}
                        <option value={String(campaign.id)}>{campaignLabel(campaign)}</option>
                    {/each}
                </select>
                {#if campaignsLoading}
                    <p class="text-xs text-muted-foreground">Loading campaigns...</p>
                {/if}
            </div>
            <div class="grid gap-2 md:col-span-2">
                <div class="flex items-center gap-1">
                    <Label>Audience Source Type</Label>
                    <HelpInfo title="Audience Source Type" what="Audience provider for simulation/publish." why="Determines how source config is parsed." how="Use supported values from capabilities." example="INLINE" />
                </div>
                <select
                    bind:value={runForm.audienceSourceType}
                    class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                    {#if capabilities?.supportedAudienceSourceTypes?.length}
                        {#each capabilities.supportedAudienceSourceTypes as sourceType}
                            <option value={sourceType}>{sourceType}</option>
                        {/each}
                    {:else}
                        <option value={runForm.audienceSourceType}>{runForm.audienceSourceType}</option>
                    {/if}
                </select>
            </div>
            <div class="grid gap-2 md:col-span-2">
                <div class="flex items-center gap-1">
                    <Label>Audience Source Config</Label>
                    <HelpInfo title="Audience Source Config" what="Input audience data/selector for simulation and publishing." why="Rule needs candidates to produce assignments." how="Use row editor; use JSON values for arrays/objects." example="participants=[userId:u1, department:ENG]" />
                </div>
                <div class="flex items-center justify-between rounded border px-3 py-2">
                    <span class="text-sm">Advanced JSON Mode</span>
                    <Switch bind:checked={advancedAudienceConfigJson} />
                </div>
                {#if advancedAudienceConfigJson}
                    <Textarea rows={8} bind:value={runForm.audienceSourceConfigText} />
                {:else}
                    <div class="space-y-2 rounded-md border p-3">
                        <p class="text-xs text-muted-foreground">
                            Provide audience source config as key/value rows. For arrays/objects, paste JSON in value.
                        </p>
                        <div class="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                            <div class="flex items-center gap-1"><span>Config Key</span><HelpInfo title="Audience Config Key" what="Audience source option name." why="Backend resolves behavior by key." how="Use expected keys for selected source type." example="participants" /></div>
                            <div class="flex items-center gap-1"><span>Config Value</span><HelpInfo title="Audience Config Value" what="Audience source option value." why="Controls who is included." how="Use scalar or JSON; arrays/objects as JSON strings." example="participants list with userId entries" /></div>
                        </div>
                        {#each audienceConfigRows as row}
                            <div class="grid grid-cols-[1fr_1fr_auto] gap-2">
                                <Input bind:value={row.key} placeholder="config key" />
                                <Input bind:value={row.value} placeholder="value (or JSON)" />
                                <Button type="button" variant="ghost" size="icon" onclick={() => removeAudienceConfigRow(row.id)}>
                                    <Trash2 class="h-4 w-4" />
                                </Button>
                            </div>
                        {/each}
                        <Button type="button" variant="outline" size="sm" onclick={addAudienceConfigRow}>
                            <Plus class="mr-2 h-4 w-4" /> Add Audience Config Row
                        </Button>
                    </div>
                {/if}
            </div>
            <div class="flex items-center gap-2">
                <Checkbox checked={runForm.replaceExistingAssignments} onCheckedChange={(v) => (runForm.replaceExistingAssignments = v)} />
                <Label>Replace Existing Assignments</Label>
                <HelpInfo title="Replace Existing Assignments" what="Overwrite current assignments in campaign." why="Needed when rule logic changed significantly." how="Enable only when full regeneration is intended." example="false" />
            </div>
            <div class="flex items-center gap-2">
                <Checkbox checked={runForm.dryRun} onCheckedChange={(v) => (runForm.dryRun = v)} />
                <Label>Dry Run (publish)</Label>
                <HelpInfo title="Publish Dry Run" what="Computes assignments without persisting them." why="Prevents accidental mass changes." how="Run true first, then false after review." example="true" />
            </div>
            <div class="flex items-center gap-2 md:col-span-2">
                <Checkbox checked={runForm.diagnosticMode} onCheckedChange={(v) => (runForm.diagnosticMode = v)} />
                <Label>Diagnostic Mode (simulate)</Label>
                <HelpInfo title="Diagnostic Mode" what="Adds exclusion diagnostics in simulation response." why="Explains why candidate pairs were not selected." how="Enable during tuning; disable for faster output." example="true" />
            </div>
        </Card.Content>
        <Card.Footer class="flex gap-2">
            <Button variant="outline" onclick={simulateRule} disabled={isBusy || !runForm.ruleId}>Simulate</Button>
            <Button onclick={publishAssignments} disabled={isBusy || !runForm.ruleId || !runForm.campaignId}>Publish Assignments</Button>
        </Card.Footer>
    </Card.Root>

    <Card.Root>
        <Card.Header class="flex flex-row items-center justify-between">
            <div>
                <div class="flex items-center gap-1">
                    <Card.Title>Rules</Card.Title>
                    <HelpInfo title="Rules List" what="Current tenant rule definitions." why="Primary way to load/edit/deprecate rules." how="Filter by status and load rule into forms." example="status=PUBLISHED" />
                </div>
                <Card.Description>List and load rule definitions</Card.Description>
            </div>
            <div class="flex items-center gap-2">
                <Input bind:value={statusFilter} placeholder="optional status filter" class="w-[200px]" />
                <Button variant="outline" size="sm" onclick={loadRules} disabled={rulesLoading}><RefreshCcw class="h-4 w-4" /></Button>
            </div>
        </Card.Header>
        <Card.Content class="space-y-2">
            {#if rulesLoading}
                <p class="text-sm text-muted-foreground">Loading rules...</p>
            {:else if rules.length === 0}
                <p class="text-sm text-muted-foreground">No rules found.</p>
            {:else}
                {#each rules as rule}
                    <div class="rounded border p-3 space-y-2">
                        <div class="flex items-center justify-between">
                            <div class="text-sm font-medium">{rule.name}</div>
                            <div class="text-xs text-muted-foreground">{rule.status}</div>
                        </div>
                        <div class="text-xs text-muted-foreground">
                            {rule.ruleType} | v{rule.semanticVersion} | updated {new Date(rule.updatedAt).toLocaleString()}
                        </div>
                        <div class="flex gap-2">
                            <Button size="sm" variant="outline" onclick={() => loadRuleToForm(rule)}>Load</Button>
                            <Button size="sm" variant="outline" onclick={() => deprecateRule(String(rule.id))}>Deprecate</Button>
                        </div>
                    </div>
                {/each}
            {/if}
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <div class="flex items-center gap-1">
                <Card.Title>Last API Response</Card.Title>
                <HelpInfo title="Last API Response" what="Response from the most recent action." why="Quick verification of returned fields and identifiers." how="Use this to inspect returned values such as IDs, status, and counts." example="id=42, status=DRAFT" />
            </div>
        </Card.Header>
        <Card.Content>
            {#if lastResponse}
                <div class="rounded-md border p-3">
                    <DataView data={lastResponse} />
                </div>
            {:else}
                <p class="text-sm text-muted-foreground">No response captured yet.</p>
            {/if}
        </Card.Content>
    </Card.Root>
</div>
