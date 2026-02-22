<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import * as Table from "$lib/components/ui/table/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import DataView from "$lib/components/data-view.svelte";
    import { Loader2, RefreshCcw } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let rules = $state<any[]>([]);
    let templates = $state<any[]>([]);
    let deliveries = $state<any[]>([]);
    let selectedRuleDetail = $state<any>(null);
    let selectedTemplateDetail = $state<any>(null);

    let loadingRules = $state(false);
    let loadingTemplates = $state(false);
    let loadingDeliveries = $state(false);

    let filters = $state({ campaignId: "", ruleId: "", status: "" });
    let lookup = $state({ ruleId: "", templateId: "" });

    let ruleForm = $state({
        id: "",
        campaignId: "",
        ruleCode: "",
        triggerType: "EVALUATION_SUBMITTED",
        audience: "EVALUATOR",
        channel: "EMAIL",
        scheduleExpr: "",
        enabled: true,
        configText: "{}",
    });

    let templateForm = $state({
        id: "",
        campaignId: "",
        templateCode: "",
        name: "",
        channel: "EMAIL",
        subject: "",
        body: "",
        requiredVariablesText: "[]",
        status: "DRAFT",
    });

    let testRenderForm = $state({
        templateId: "",
        recipient: "",
        variablesText: "{}",
    });
    let testRenderResult = $state<any>(null);

    function parseJson<T>(text: string, fallback: T): T {
        const trimmed = text?.trim();
        if (!trimmed) return fallback;
        return JSON.parse(trimmed) as T;
    }

    async function loadRules() {
        loadingRules = true;
        try {
            const res = await api.get("/admin/notifications/rules", {
                params: { campaignId: filters.campaignId || undefined },
            });
            rules = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load rules");
        } finally {
            loadingRules = false;
        }
    }

    async function loadTemplates() {
        loadingTemplates = true;
        try {
            const res = await api.get("/admin/notifications/templates", {
                params: { campaignId: filters.campaignId || undefined },
            });
            templates = res.data;
            if (!testRenderForm.templateId && templates.length > 0) {
                testRenderForm.templateId = String(templates[0].id);
            }
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load templates");
        } finally {
            loadingTemplates = false;
        }
    }

    async function loadDeliveries() {
        loadingDeliveries = true;
        try {
            const params: Record<string, unknown> = {};
            if (filters.campaignId.trim()) params.campaignId = filters.campaignId.trim();
            if (filters.ruleId.trim()) params.ruleId = Number(filters.ruleId);
            if (filters.status.trim()) params.status = filters.status.trim();

            const res = await api.get("/admin/notifications/deliveries", { params });
            deliveries = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load deliveries");
        } finally {
            loadingDeliveries = false;
        }
    }

    async function saveRule() {
        try {
            const payload = {
                campaignId: ruleForm.campaignId,
                ruleCode: ruleForm.ruleCode,
                triggerType: ruleForm.triggerType,
                audience: ruleForm.audience,
                channel: ruleForm.channel,
                scheduleExpr: ruleForm.scheduleExpr || null,
                enabled: ruleForm.enabled,
                config: parseJson<Record<string, unknown>>(ruleForm.configText, {}),
            };
            if (ruleForm.id) {
                await api.put(`/admin/notifications/rules/${ruleForm.id}`, payload);
                toast.success("Rule updated");
            } else {
                await api.post("/admin/notifications/rules", payload);
                toast.success("Rule created");
            }
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to save rule");
        }
    }

    function loadRuleToForm(rule: any) {
        ruleForm = {
            id: String(rule.id),
            campaignId: rule.campaignId,
            ruleCode: rule.ruleCode,
            triggerType: rule.triggerType,
            audience: rule.audience,
            channel: rule.channel,
            scheduleExpr: rule.scheduleExpr || "",
            enabled: !!rule.enabled,
            configText: JSON.stringify(rule.config || {}, null, 2),
        };
    }

    async function deleteRule(id: number) {
        if (!confirm(`Delete rule ${id}?`)) return;
        try {
            await api.delete(`/admin/notifications/rules/${id}`);
            toast.success("Rule deleted");
            await loadRules();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to delete rule");
        }
    }

    async function loadRuleById() {
        if (!lookup.ruleId.trim()) return;
        try {
            const res = await api.get(`/admin/notifications/rules/${lookup.ruleId.trim()}`);
            selectedRuleDetail = res.data;
            loadRuleToForm(res.data);
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load rule by id");
        }
    }

    async function saveTemplate() {
        try {
            const payload = {
                campaignId: templateForm.campaignId || null,
                templateCode: templateForm.templateCode,
                name: templateForm.name,
                channel: templateForm.channel,
                subject: templateForm.subject || null,
                body: templateForm.body,
                requiredVariables: parseJson<string[]>(templateForm.requiredVariablesText, []),
                status: templateForm.status,
            };
            if (templateForm.id) {
                await api.put(`/admin/notifications/templates/${templateForm.id}`, payload);
                toast.success("Template updated");
            } else {
                await api.post("/admin/notifications/templates", payload);
                toast.success("Template created");
            }
            await loadTemplates();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to save template");
        }
    }

    function loadTemplateToForm(template: any) {
        templateForm = {
            id: String(template.id),
            campaignId: template.campaignId || "",
            templateCode: template.templateCode,
            name: template.name,
            channel: template.channel,
            subject: template.subject || "",
            body: template.body,
            requiredVariablesText: JSON.stringify(template.requiredVariables || [], null, 2),
            status: template.status,
        };
        testRenderForm.templateId = String(template.id);
    }

    async function deleteTemplate(id: number) {
        if (!confirm(`Delete template ${id}?`)) return;
        try {
            await api.delete(`/admin/notifications/templates/${id}`);
            toast.success("Template deleted");
            await loadTemplates();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to delete template");
        }
    }

    async function loadTemplateById() {
        if (!lookup.templateId.trim()) return;
        try {
            const res = await api.get(`/admin/notifications/templates/${lookup.templateId.trim()}`);
            selectedTemplateDetail = res.data;
            loadTemplateToForm(res.data);
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load template by id");
        }
    }

    async function testRender() {
        if (!testRenderForm.templateId || !testRenderForm.recipient.trim()) return;
        try {
            const payload = {
                recipient: testRenderForm.recipient,
                variables: parseJson<Record<string, unknown>>(testRenderForm.variablesText, {}),
            };
            const res = await api.post(`/admin/notifications/templates/${testRenderForm.templateId}/test-render`, payload);
            testRenderResult = res.data;
            toast.success("Template render success");
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to render template");
        }
    }

    async function retryDelivery(id: number) {
        try {
            await api.post(`/admin/notifications/deliveries/${id}/retry`);
            toast.success("Retry requested");
            await loadDeliveries();
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Retry failed");
        }
    }

    function resetRuleForm() {
        ruleForm = {
            id: "",
            campaignId: filters.campaignId || "",
            ruleCode: "",
            triggerType: "EVALUATION_SUBMITTED",
            audience: "EVALUATOR",
            channel: "EMAIL",
            scheduleExpr: "",
            enabled: true,
            configText: "{}",
        };
    }

    function resetTemplateForm() {
        templateForm = {
            id: "",
            campaignId: filters.campaignId || "",
            templateCode: "",
            name: "",
            channel: "EMAIL",
            subject: "",
            body: "",
            requiredVariablesText: "[]",
            status: "DRAFT",
        };
    }

    async function refreshAll() {
        await Promise.all([loadRules(), loadTemplates(), loadDeliveries()]);
    }

    onMount(refreshAll);
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">Notification Rule Engine</h1>
        <p class="text-muted-foreground">Admin management for notification rules, templates, render tests, and delivery retries.</p>
    </div>

    <Card.Root>
        <Card.Header>
            <Card.Title>Global Filters</Card.Title>
        </Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-4">
            <Input bind:value={filters.campaignId} placeholder="campaignId" />
            <Input bind:value={filters.ruleId} placeholder="ruleId (deliveries)" />
            <Input bind:value={filters.status} placeholder="status (deliveries)" />
            <Button variant="outline" onclick={refreshAll}>
                <RefreshCcw class="mr-2 h-4 w-4" />Refresh
            </Button>
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header>
            <Card.Title>Direct Lookup</Card.Title>
            <Card.Description>Explicit GET by id endpoints for rules/templates.</Card.Description>
        </Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-4">
            <Input bind:value={lookup.ruleId} placeholder="rule id" />
            <Button variant="outline" onclick={loadRuleById} disabled={!lookup.ruleId.trim()}>Load Rule</Button>
            <Input bind:value={lookup.templateId} placeholder="template id" />
            <Button variant="outline" onclick={loadTemplateById} disabled={!lookup.templateId.trim()}>Load Template</Button>
            {#if selectedRuleDetail}
                <div class="rounded-md border bg-muted/30 p-3 md:col-span-2">
                    <p class="text-sm font-medium mb-2">Rule Detail</p>
                    <DataView data={selectedRuleDetail} />
                </div>
            {/if}
            {#if selectedTemplateDetail}
                <div class="rounded-md border bg-muted/30 p-3 md:col-span-2">
                    <p class="text-sm font-medium mb-2">Template Detail</p>
                    <DataView data={selectedTemplateDetail} />
                </div>
            {/if}
        </Card.Content>
    </Card.Root>

    <div class="grid gap-6 xl:grid-cols-2">
        <Card.Root>
            <Card.Header>
                <Card.Title>Rule Form</Card.Title>
            </Card.Header>
            <Card.Content class="grid gap-3">
                <Input bind:value={ruleForm.id} placeholder="id (blank=create)" />
                <Input bind:value={ruleForm.campaignId} placeholder="campaignId" />
                <Input bind:value={ruleForm.ruleCode} placeholder="ruleCode" />
                <Input bind:value={ruleForm.triggerType} placeholder="triggerType" />
                <Input bind:value={ruleForm.audience} placeholder="audience" />
                <Input bind:value={ruleForm.channel} placeholder="channel" />
                <Input bind:value={ruleForm.scheduleExpr} placeholder="scheduleExpr" />
                <label class="text-sm flex items-center gap-2"><input type="checkbox" bind:checked={ruleForm.enabled} />enabled</label>
                <Textarea bind:value={ruleForm.configText} rows={8} placeholder='config JSON' />
            </Card.Content>
            <Card.Footer class="flex gap-2">
                <Button onclick={saveRule}>Save</Button>
                <Button variant="outline" onclick={resetRuleForm}>Reset</Button>
            </Card.Footer>
        </Card.Root>

        <Card.Root>
            <Card.Header>
                <Card.Title>Rules</Card.Title>
                <Card.Description>{rules.length} rule(s)</Card.Description>
            </Card.Header>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row><Table.Head>ID</Table.Head><Table.Head>Code</Table.Head><Table.Head>Trigger</Table.Head><Table.Head>Audience</Table.Head><Table.Head>Enabled</Table.Head><Table.Head>Actions</Table.Head></Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loadingRules}
                            <Table.Row><Table.Cell colspan={6}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                        {:else if rules.length === 0}
                            <Table.Row><Table.Cell colspan={6}>No rules</Table.Cell></Table.Row>
                        {:else}
                            {#each rules as rule}
                                <Table.Row>
                                    <Table.Cell>{rule.id}</Table.Cell>
                                    <Table.Cell>{rule.ruleCode}</Table.Cell>
                                    <Table.Cell>{rule.triggerType}</Table.Cell>
                                    <Table.Cell>{rule.audience}</Table.Cell>
                                    <Table.Cell>{rule.enabled ? "true" : "false"}</Table.Cell>
                                    <Table.Cell class="space-x-2">
                                        <Button size="sm" variant="outline" onclick={() => loadRuleToForm(rule)}>Edit</Button>
                                        <Button size="sm" variant="destructive" onclick={() => deleteRule(rule.id)}>Delete</Button>
                                    </Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>
    </div>

    <div class="grid gap-6 xl:grid-cols-2">
        <Card.Root>
            <Card.Header><Card.Title>Template Form</Card.Title></Card.Header>
            <Card.Content class="grid gap-3">
                <Input bind:value={templateForm.id} placeholder="id (blank=create)" />
                <Input bind:value={templateForm.campaignId} placeholder="campaignId (optional)" />
                <Input bind:value={templateForm.templateCode} placeholder="templateCode" />
                <Input bind:value={templateForm.name} placeholder="name" />
                <Input bind:value={templateForm.channel} placeholder="channel" />
                <Input bind:value={templateForm.status} placeholder="status" />
                <Input bind:value={templateForm.subject} placeholder="subject" />
                <Textarea bind:value={templateForm.body} rows={5} placeholder='body' />
                <Textarea bind:value={templateForm.requiredVariablesText} rows={4} placeholder='requiredVariables JSON array' />
            </Card.Content>
            <Card.Footer class="flex gap-2">
                <Button onclick={saveTemplate}>Save</Button>
                <Button variant="outline" onclick={resetTemplateForm}>Reset</Button>
            </Card.Footer>
        </Card.Root>

        <Card.Root>
            <Card.Header><Card.Title>Templates</Card.Title><Card.Description>{templates.length} template(s)</Card.Description></Card.Header>
            <Card.Content>
                <Table.Root>
                    <Table.Header>
                        <Table.Row><Table.Head>ID</Table.Head><Table.Head>Code</Table.Head><Table.Head>Name</Table.Head><Table.Head>Status</Table.Head><Table.Head>Actions</Table.Head></Table.Row>
                    </Table.Header>
                    <Table.Body>
                        {#if loadingTemplates}
                            <Table.Row><Table.Cell colspan={5}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                        {:else if templates.length === 0}
                            <Table.Row><Table.Cell colspan={5}>No templates</Table.Cell></Table.Row>
                        {:else}
                            {#each templates as template}
                                <Table.Row>
                                    <Table.Cell>{template.id}</Table.Cell>
                                    <Table.Cell>{template.templateCode}</Table.Cell>
                                    <Table.Cell>{template.name}</Table.Cell>
                                    <Table.Cell>{template.status}</Table.Cell>
                                    <Table.Cell class="space-x-2">
                                        <Button size="sm" variant="outline" onclick={() => loadTemplateToForm(template)}>Edit</Button>
                                        <Button size="sm" variant="destructive" onclick={() => deleteTemplate(template.id)}>Delete</Button>
                                    </Table.Cell>
                                </Table.Row>
                            {/each}
                        {/if}
                    </Table.Body>
                </Table.Root>
            </Card.Content>
        </Card.Root>
    </div>

    <Card.Root>
        <Card.Header><Card.Title>Template Test Render</Card.Title></Card.Header>
        <Card.Content class="grid gap-3 md:grid-cols-4">
            <Input bind:value={testRenderForm.templateId} placeholder="templateId" />
            <Input bind:value={testRenderForm.recipient} placeholder="recipient" />
            <Textarea bind:value={testRenderForm.variablesText} rows={3} placeholder='variables JSON' class="md:col-span-2" />
            <Button onclick={testRender} disabled={!testRenderForm.templateId || !testRenderForm.recipient}>Render</Button>
            {#if testRenderResult}
                <div class="rounded-md border bg-muted/30 p-3 md:col-span-4">
                    <DataView data={testRenderResult} />
                </div>
            {/if}
        </Card.Content>
    </Card.Root>

    <Card.Root>
        <Card.Header><Card.Title>Delivery Logs</Card.Title><Card.Description>{deliveries.length} delivery record(s)</Card.Description></Card.Header>
        <Card.Content>
            <Table.Root>
                <Table.Header>
                    <Table.Row><Table.Head>ID</Table.Head><Table.Head>Campaign</Table.Head><Table.Head>Rule</Table.Head><Table.Head>Recipient</Table.Head><Table.Head>Channel</Table.Head><Table.Head>Status</Table.Head><Table.Head>Action</Table.Head></Table.Row>
                </Table.Header>
                <Table.Body>
                    {#if loadingDeliveries}
                        <Table.Row><Table.Cell colspan={7}><Loader2 class="h-4 w-4 animate-spin" /></Table.Cell></Table.Row>
                    {:else if deliveries.length === 0}
                        <Table.Row><Table.Cell colspan={7}>No deliveries</Table.Cell></Table.Row>
                    {:else}
                        {#each deliveries as d}
                            <Table.Row>
                                <Table.Cell>{d.id}</Table.Cell>
                                <Table.Cell>{d.campaignId || "-"}</Table.Cell>
                                <Table.Cell>{d.ruleId || "-"}</Table.Cell>
                                <Table.Cell>{d.recipient}</Table.Cell>
                                <Table.Cell>{d.channel}</Table.Cell>
                                <Table.Cell>{d.status}</Table.Cell>
                                <Table.Cell>
                                    <Button size="sm" variant="outline" disabled={d.status !== "FAILED"} onclick={() => retryDelivery(d.id)}>Retry</Button>
                                </Table.Cell>
                            </Table.Row>
                        {/each}
                    {/if}
                </Table.Body>
            </Table.Root>
        </Card.Content>
    </Card.Root>
</div>
