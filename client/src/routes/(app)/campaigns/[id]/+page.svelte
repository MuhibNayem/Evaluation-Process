<script lang="ts">
    import { page } from "$app/stores";
    import { goto } from "$app/navigation";
    import api from "$lib/api.js";
    import { onMount } from "svelte";
    import { Button } from "$lib/components/ui/button/index.js";
    import DatePicker from "$lib/components/ui/date-picker/date-picker.svelte";
    import { Badge } from "$lib/components/ui/badge/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Separator } from "$lib/components/ui/separator/index.js";
    import * as AlertDialog from "$lib/components/ui/alert-dialog/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import {
        Select,
        SelectContent,
        SelectItem,
        SelectTrigger,
    } from "$lib/components/ui/select/index.js";
    import { toast } from "svelte-sonner";
    import {
        ArrowLeft,
        Loader2,
        Edit,
        Archive,
        Play,
        StopCircle,
        Calendar,
        CalendarClock,
        UserPlus,
        Sparkles,
    } from "@lucide/svelte";
    import AudienceBuilder from "$lib/components/campaign/AudienceBuilder.svelte";
    import {
        defaultRuleConfig,
        normalizeRuleConfig,
        participantsFromSourceConfig,
        sourceConfigFromParticipants,
        type AudienceParticipant,
    } from "$lib/campaign/audience-builder.js";

    let campaignId = $state($page.params.id);
    let campaign = $state<any>(null);
    let loading = $state(true);
    let error = $state<string | null>(null);
    let isProcessing = $state(false);

    let showExtendDialog = $state(false);
    let newEndDate = $state("");
    let showAssignDialog = $state(false);
    let assignEvaluatorId = $state("");
    let assignEvaluateeId = $state("");
    let assignRole = $state("PEER");

    let showDynamicDialog = $state(false);
    let dynamicAudienceSourceType = $state("INLINE");
    let dynamicAssignmentRuleType = $state("ATTRIBUTE_MATCH");
    let dynamicParticipants = $state<AudienceParticipant[]>([]);
    let dynamicRuleConfig = $state<Record<string, unknown>>(defaultRuleConfig("ATTRIBUTE_MATCH"));
    let dynamicReplaceExisting = $state(false);
    let dynamicDryRun = $state(true);

    async function fetchCampaign() {
        loading = true;
        try {
            const res = await api.get(`/campaigns/${campaignId}`);
            campaign = res.data;
            dynamicAudienceSourceType = campaign.audienceSourceType || "INLINE";
            dynamicAssignmentRuleType = campaign.assignmentRuleType || "ATTRIBUTE_MATCH";
            dynamicParticipants = participantsFromSourceConfig(campaign.audienceSourceConfig);
            dynamicRuleConfig = normalizeRuleConfig(
                dynamicAssignmentRuleType,
                campaign.assignmentRuleConfig,
            );
        } catch (err: any) {
            console.error(err);
            error = "Failed to load campaign.";
            toast.error("Failed to load campaign");
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        fetchCampaign();
    });

    async function handleExtendDeadline() {
        if (!newEndDate) return;
        isProcessing = true;
        try {
            const res = await api.post(`/campaigns/${campaignId}/extend-deadline`, {
                newEndDate: new Date(newEndDate).toISOString(),
            });
            campaign = res.data;
            toast.success("Deadline extended.");
            showExtendDialog = false;
        } catch (err: any) {
            toast.error(
                "Failed to extend deadline: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleAddAssignments() {
        if (!assignEvaluatorId || !assignEvaluateeId) {
            toast.error("Please fill in all fields");
            return;
        }
        isProcessing = true;
        try {
            const payload = {
                assignments: [
                    {
                        evaluatorId: assignEvaluatorId,
                        evaluateeId: assignEvaluateeId,
                        evaluatorRole: assignRole,
                    },
                ],
            };
            const res = await api.post(`/campaigns/${campaignId}/assignments`, payload);
            campaign = res.data;
            toast.success("Assignment added.");
            showAssignDialog = false;
            assignEvaluatorId = "";
            assignEvaluateeId = "";
            assignRole = "PEER";
        } catch (err: any) {
            toast.error(
                "Failed to add assignments: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleGenerateDynamicAssignments() {
        isProcessing = true;
        try {
            const payload = {
                audienceSourceType: dynamicAudienceSourceType,
                audienceSourceConfig: sourceConfigFromParticipants(dynamicParticipants),
                assignmentRuleType: dynamicAssignmentRuleType,
                assignmentRuleConfig: normalizeRuleConfig(
                    dynamicAssignmentRuleType,
                    dynamicRuleConfig,
                ),
                replaceExistingAssignments: dynamicReplaceExisting,
                dryRun: dynamicDryRun,
            };

            const res = await api.post(`/campaigns/${campaignId}/assignments/dynamic`, payload);
            const generatedCount = res.data?.generatedCount ?? 0;

            if (dynamicDryRun) {
                toast.success(`Dry-run complete. ${generatedCount} assignments would be generated.`);
            } else {
                toast.success(`Generated ${generatedCount} assignments.`);
                await fetchCampaign();
            }
            showDynamicDialog = false;
        } catch (err: any) {
            toast.error(
                "Failed to generate dynamic assignments: " +
                    (err.response?.data?.detail || err.message || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleActivate() {
        isProcessing = true;
        try {
            const res = await api.post(`/campaigns/${campaignId}/activate`);
            campaign = res.data;
            toast.success("Campaign activated!");
        } catch (err: any) {
            toast.error(
                "Failed to activate: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleClose() {
        isProcessing = true;
        try {
            const res = await api.post(`/campaigns/${campaignId}/close`);
            campaign = res.data;
            toast.success("Campaign closed.");
        } catch (err: any) {
            toast.error(
                "Failed to close: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    async function handleArchive() {
        isProcessing = true;
        try {
            const res = await api.post(`/campaigns/${campaignId}/archive`);
            campaign = res.data;
            toast.success("Campaign archived.");
        } catch (err: any) {
            toast.error(
                "Failed to archive: " +
                    (err.response?.data?.detail || "Unknown error"),
            );
        } finally {
            isProcessing = false;
        }
    }

    function getStatusColor(status: string) {
        switch (status) {
            case "ACTIVE":
                return "bg-green-500";
            case "SCHEDULED":
                return "bg-blue-500";
            case "CLOSED":
                return "bg-gray-500";
            case "ARCHIVED":
                return "bg-gray-300 text-gray-700";
            default:
                return "bg-yellow-500";
        }
    }

    function formatDate(dateStr: string) {
        if (!dateStr) return "-";
        return new Date(dateStr).toLocaleString();
    }
</script>

<div class="flex flex-col gap-6 pb-20">
    <div class="flex items-center gap-4">
        <Button variant="ghost" size="icon" href="/campaigns" onclick={() => goto("/campaigns")}>
            <ArrowLeft class="h-4 w-4" />
        </Button>
        {#if campaign}
            <div class="flex flex-col">
                <div class="flex items-center gap-3">
                    <h1 class="text-lg font-semibold md:text-2xl">{campaign.name}</h1>
                    <Badge class={getStatusColor(campaign.status)}>{campaign.status}</Badge>
                </div>
                <div class="flex items-center gap-2 text-sm text-muted-foreground mt-1">
                    <Calendar class="h-3 w-3" />
                    <span>{new Date(campaign.startDate).toLocaleDateString()} - {new Date(campaign.endDate).toLocaleDateString()}</span>
                </div>
            </div>
        {:else if loading}
            <h1 class="text-lg font-semibold md:text-2xl">Loading...</h1>
        {:else}
            <h1 class="text-lg font-semibold md:text-2xl text-red-500">Error</h1>
        {/if}

        <div class="ml-auto flex items-center gap-2">
            {#if campaign}
                {#if campaign.status === "DRAFT" || campaign.status === "SCHEDULED"}
                    <Button
                        variant="outline"
                        size="sm"
                        href={`/campaigns/${campaignId}/edit`}
                        onclick={() => goto(`/campaigns/${campaignId}/edit`)}
                    >
                        <Edit class="mr-2 h-4 w-4" /> Edit
                    </Button>
                {/if}

                {#if campaign.status === "DRAFT" || campaign.status === "SCHEDULED"}
                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button variant="default" size="sm" disabled={isProcessing}>
                                {#if isProcessing}
                                    <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                                {:else}
                                    <Play class="mr-2 h-4 w-4" />
                                {/if}
                                Activate
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title>Activate Campaign?</AlertDialog.Title>
                                <AlertDialog.Description>
                                    This will make the campaign active immediately and notify evaluators.
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action onclick={handleActivate}>Activate</AlertDialog.Action>
                            </AlertDialog.Footer>
                        </AlertDialog.Content>
                    </AlertDialog.Root>
                {/if}

                {#if campaign.status === "ACTIVE"}
                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button variant="destructive" size="sm" disabled={isProcessing}>
                                <StopCircle class="mr-2 h-4 w-4" /> Close
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title>Close Campaign?</AlertDialog.Title>
                                <AlertDialog.Description>
                                    This will stop all evaluations. No further submissions will be accepted.
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action onclick={handleClose}>Close</AlertDialog.Action>
                            </AlertDialog.Footer>
                        </AlertDialog.Content>
                    </AlertDialog.Root>
                {/if}

                {#if campaign.status === "CLOSED"}
                    <AlertDialog.Root>
                        <AlertDialog.Trigger>
                            <Button variant="secondary" size="sm" disabled={isProcessing}>
                                <Archive class="mr-2 h-4 w-4" /> Archive
                            </Button>
                        </AlertDialog.Trigger>
                        <AlertDialog.Content>
                            <AlertDialog.Header>
                                <AlertDialog.Title>Archive Campaign?</AlertDialog.Title>
                                <AlertDialog.Description>
                                    Archived campaigns are hidden from the main list.
                                </AlertDialog.Description>
                            </AlertDialog.Header>
                            <AlertDialog.Footer>
                                <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                <AlertDialog.Action onclick={handleArchive}>Archive</AlertDialog.Action>
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
        <div class="rounded-md bg-red-50 p-4 text-red-700 dark:bg-red-900/30 dark:text-red-400">{error}</div>
    {:else if campaign}
        <div class="grid gap-6 md:grid-cols-2">
            <Card.Root>
                <Card.Header>
                    <div class="flex items-center justify-between">
                        <Card.Title>Details</Card.Title>
                        {#if campaign.status === "ACTIVE"}
                            <div class="flex gap-2">
                                <AlertDialog.Root bind:open={showExtendDialog}>
                                    <AlertDialog.Trigger>
                                        <Button variant="outline" size="sm">
                                            <CalendarClock class="mr-2 h-4 w-4" /> Extend
                                        </Button>
                                    </AlertDialog.Trigger>
                                    <AlertDialog.Content>
                                        <AlertDialog.Header>
                                            <AlertDialog.Title>Extend Deadline</AlertDialog.Title>
                                            <AlertDialog.Description>
                                                Choose a new end date for this campaign.
                                            </AlertDialog.Description>
                                        </AlertDialog.Header>
                                        <div class="py-4">
                                            <div class="grid gap-2">
                                                <label for="end-date" class="text-sm font-medium">New End Date</label>
                                                <DatePicker bind:value={newEndDate} />
                                            </div>
                                        </div>
                                        <AlertDialog.Footer>
                                            <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                            <AlertDialog.Action onclick={handleExtendDeadline}>Extend</AlertDialog.Action>
                                        </AlertDialog.Footer>
                                    </AlertDialog.Content>
                                </AlertDialog.Root>
                            </div>
                        {/if}

                        {#if campaign.status === "DRAFT" || campaign.status === "SCHEDULED"}
                            <div class="flex gap-2">
                                <AlertDialog.Root bind:open={showAssignDialog}>
                                    <AlertDialog.Trigger>
                                        <Button variant="outline" size="sm">
                                            <UserPlus class="mr-2 h-4 w-4" /> Manual Assign
                                        </Button>
                                    </AlertDialog.Trigger>
                                    <AlertDialog.Content>
                                        <AlertDialog.Header>
                                            <AlertDialog.Title>Add Assignment</AlertDialog.Title>
                                            <AlertDialog.Description>
                                                Manually assign one evaluator to one evaluatee.
                                            </AlertDialog.Description>
                                        </AlertDialog.Header>
                                        <div class="py-4 grid gap-4">
                                            <div class="grid gap-2">
                                                <label for="evaluator-id" class="text-sm font-medium">Evaluator ID</label>
                                                <Input id="evaluator-id" bind:value={assignEvaluatorId} />
                                            </div>
                                            <div class="grid gap-2">
                                                <label for="evaluatee-id" class="text-sm font-medium">Evaluatee ID</label>
                                                <Input id="evaluatee-id" bind:value={assignEvaluateeId} />
                                            </div>
                                            <div class="grid gap-2">
                                                <label for="role" class="text-sm font-medium">Role</label>
                                                <Select type="single" bind:value={assignRole} onValueChange={(v) => (assignRole = v)}>
                                                    <SelectTrigger>{assignRole || "Select role"}</SelectTrigger>
                                                    <SelectContent>
                                                        <SelectItem value="SELF">SELF</SelectItem>
                                                        <SelectItem value="PEER">PEER</SelectItem>
                                                        <SelectItem value="SUPERVISOR">SUPERVISOR</SelectItem>
                                                        <SelectItem value="SUBORDINATE">SUBORDINATE</SelectItem>
                                                        <SelectItem value="EXTERNAL">EXTERNAL</SelectItem>
                                                    </SelectContent>
                                                </Select>
                                            </div>
                                        </div>
                                        <AlertDialog.Footer>
                                            <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                            <AlertDialog.Action onclick={handleAddAssignments}>Add</AlertDialog.Action>
                                        </AlertDialog.Footer>
                                    </AlertDialog.Content>
                                </AlertDialog.Root>

                                <AlertDialog.Root bind:open={showDynamicDialog}>
                                    <AlertDialog.Trigger>
                                        <Button variant="outline" size="sm">
                                            <Sparkles class="mr-2 h-4 w-4" /> Dynamic Assign
                                        </Button>
                                    </AlertDialog.Trigger>
                                    <AlertDialog.Content>
                                        <AlertDialog.Header>
                                            <AlertDialog.Title>Generate Dynamic Assignments</AlertDialog.Title>
                                            <AlertDialog.Description>
                                                Configure audience source and rule, then run dry-run or persist.
                                            </AlertDialog.Description>
                                        </AlertDialog.Header>
                                        <div class="py-4 grid gap-3">
                                            <AudienceBuilder
                                                bind:audienceSourceType={dynamicAudienceSourceType}
                                                bind:assignmentRuleType={dynamicAssignmentRuleType}
                                                bind:participants={dynamicParticipants}
                                                bind:ruleConfig={dynamicRuleConfig}
                                                title="Audience Builder"
                                                disabled={isProcessing}
                                            />

                                            <div class="grid gap-2 md:grid-cols-2">
                                                <div class="flex items-center space-x-2">
                                                    <Checkbox
                                                        id="replace-existing-assignments"
                                                        checked={dynamicReplaceExisting}
                                                        onCheckedChange={(v) => (dynamicReplaceExisting = v)}
                                                    />
                                                    <label for="replace-existing-assignments" class="text-sm">Replace existing assignments</label>
                                                </div>
                                                <div class="flex items-center space-x-2">
                                                    <Checkbox
                                                        id="dry-run-assignments"
                                                        checked={dynamicDryRun}
                                                        onCheckedChange={(v) => (dynamicDryRun = v)}
                                                    />
                                                    <label for="dry-run-assignments" class="text-sm">Dry-run only</label>
                                                </div>
                                            </div>
                                        </div>
                                        <AlertDialog.Footer>
                                            <AlertDialog.Cancel>Cancel</AlertDialog.Cancel>
                                            <AlertDialog.Action onclick={handleGenerateDynamicAssignments}>
                                                Run
                                            </AlertDialog.Action>
                                        </AlertDialog.Footer>
                                    </AlertDialog.Content>
                                </AlertDialog.Root>
                            </div>
                        {/if}
                    </div>
                </Card.Header>
                <Card.Content class="space-y-4">
                    <div class="grid gap-1">
                        <span class="text-sm font-medium text-muted-foreground">Description</span>
                        <p class="text-sm">{campaign.description || "No description"}</p>
                    </div>
                    <Separator />
                    <div class="grid gap-1">
                        <span class="text-sm font-medium text-muted-foreground">Template</span>
                        <p class="text-sm font-mono">{campaign.templateId}</p>
                        <p class="text-xs text-muted-foreground">Version {campaign.templateVersion}</p>
                    </div>
                    <Separator />
                    <div class="grid grid-cols-2 gap-4">
                        <div>
                            <span class="text-sm font-medium text-muted-foreground">Scoring</span>
                            <p class="text-sm">{campaign.scoringMethod}</p>
                        </div>
                        <div>
                            <span class="text-sm font-medium text-muted-foreground">Min Respondents</span>
                            <p class="text-sm">{campaign.minimumRespondents}</p>
                        </div>
                    </div>
                    <Separator />
                    <div class="grid gap-1">
                        <span class="text-sm font-medium text-muted-foreground">Dynamic Source</span>
                        <p class="text-sm">{campaign.audienceSourceType || "Not configured"}</p>
                    </div>
                    <div class="grid gap-1">
                        <span class="text-sm font-medium text-muted-foreground">Dynamic Rule</span>
                        <p class="text-sm">{campaign.assignmentRuleType || "Not configured"}</p>
                    </div>
                    <Separator />
                    <div class="grid gap-1">
                        <span class="text-sm font-medium text-muted-foreground">Anonymous Mode</span>
                        <Badge variant={campaign.anonymousMode ? "default" : "secondary"} class="w-fit">
                            {campaign.anonymousMode ? "Enabled" : "Disabled"}
                        </Badge>
                    </div>
                </Card.Content>
            </Card.Root>

            <Card.Root>
                <Card.Header>
                    <Card.Title>Progress</Card.Title>
                </Card.Header>
                <Card.Content class="space-y-4">
                    <div class="flex flex-col gap-2">
                        <div class="flex justify-between text-sm">
                            <span>Completion</span>
                            <span class="font-bold">{campaign.completionPercentage.toFixed(1)}%</span>
                        </div>
                        <div class="h-2 w-full rounded-full bg-secondary">
                            <div
                                class="h-full rounded-full bg-primary transition-all duration-500"
                                style={`width: ${campaign.completionPercentage}%`}
                            ></div>
                        </div>
                    </div>
                    <div class="grid grid-cols-2 gap-4 pt-4">
                        <div class="rounded-lg border p-3 text-center">
                            <div class="text-2xl font-bold">{campaign.completedAssignments}</div>
                            <div class="text-xs text-muted-foreground">Completed</div>
                        </div>
                        <div class="rounded-lg border p-3 text-center">
                            <div class="text-2xl font-bold">{campaign.totalAssignments}</div>
                            <div class="text-xs text-muted-foreground">Total Assignments</div>
                        </div>
                    </div>
                    <Separator />
                    <div class="grid gap-1 text-sm">
                        <span class="text-muted-foreground">Start</span>
                        <span>{formatDate(campaign.startDate)}</span>
                        <span class="text-muted-foreground mt-2">End</span>
                        <span>{formatDate(campaign.endDate)}</span>
                    </div>
                </Card.Content>
            </Card.Root>
        </div>
    {/if}
</div>
