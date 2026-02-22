<script lang="ts">
    import { page } from "$app/stores";
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { goto } from "$app/navigation";
    import EvaluationForm from "$lib/components/evaluation/EvaluationForm.svelte";
    import { Loader2, ArrowLeft, Flag, Ban } from "@lucide/svelte";
    import { Button } from "$lib/components/ui/button/index.js";
    import { toast } from "svelte-sonner";

    import { Badge } from "$lib/components/ui/badge/index.js";
    import DataView from "$lib/components/data-view.svelte";

    // @ts-ignore
    let evaluationId = $derived($page.params.id);

    let isLoading = $state(true);
    let isSubmitting = $state(false);
    let processingAction = $state(false);
    let error = $state<string | null>(null);

    let evaluation = $state<any>(null);
    let template = $state<any>(null);
    let initialAnswers = $state<Record<string, any>>({});
    let mode = $state<"create" | "edit" | "view">("edit");
    let adminDetail = $state<any>(null);

    async function fetchData() {
        isLoading = true;
        try {
            // 1. Fetch Evaluation
            const evalRes = await api.get(`/evaluations/${evaluationId}`);
            evaluation = evalRes.data;

            // Determine mode
            if (
                evaluation.status === "COMPLETED" ||
                evaluation.status === "SUBMITTED" ||
                evaluation.status === "FLAGGED" ||
                evaluation.status === "INVALIDATED"
            ) {
                mode = "view";
            } else {
                mode = "edit";
            }

            // 2. Fetch Template
            const templateRes = await api.get(
                `/templates/${evaluation.templateId}`,
            );
            template = templateRes.data;

            // 3. Map answers
            if (evaluation.answers) {
                evaluation.answers.forEach((a: any) => {
                    initialAnswers[a.questionId] = {
                        questionId: a.questionId,
                        value: a.value,
                        selectedOptions: a.selectedOptions,
                        textResponse: a.textResponse,
                    };
                });
            }

            try {
                const adminRes = await api.get(`/evaluations/${evaluationId}/admin-detail`);
                adminDetail = adminRes.data;
            } catch {
                adminDetail = null;
            }
        } catch (err) {
            console.error(err);
            error = "Failed to load evaluation.";
            toast.error("Failed to load evaluation");
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        if (evaluationId) fetchData();
    });

    async function handleSubmit(answersList: any[]) {
        if (mode === "view") return;
        isSubmitting = true;
        try {
            const payload = {
                answers: answersList.map((a) => ({
                    questionId: a.questionId,
                    value: a.value?.toString(),
                    selectedOptions: a.selectedOptions,
                    textResponse: a.textResponse,
                    metadata: {},
                })),
            };

            await api.put(`/evaluations/${evaluationId}`, payload);
            // Reload data to reflect changes
            await fetchData();
            toast.success("Evaluation saved!");
        } catch (err: any) {
            console.error(err);
            error = "Failed to submit evaluation.";
            toast.error("Failed to submit evaluation");
        } finally {
            isSubmitting = false;
        }
    }

    async function handleFlag() {
        if (!confirm("Are you sure you want to flag this evaluation?")) return;
        processingAction = true;
        try {
            await api.post(`/evaluations/${evaluationId}/flag`);
            await fetchData();
            toast.success("Evaluation flagged.");
        } catch (err) {
            console.error(err);
            toast.error("Failed to flag evaluation.");
        } finally {
            processingAction = false;
        }
    }

    async function handleInvalidate() {
        if (!confirm("Are you sure you want to invalidate this evaluation?"))
            return;
        processingAction = true;
        try {
            await api.post(`/evaluations/${evaluationId}/invalidate`);
            await fetchData();
            toast.success("Evaluation invalidated.");
        } catch (err) {
            console.error(err);
            toast.error("Failed to invalidate evaluation.");
        } finally {
            processingAction = false;
        }
    }

    async function handleReopen() {
        processingAction = true;
        try {
            await api.post(`/evaluations/${evaluationId}/reopen`);
            await fetchData();
            toast.success("Evaluation reopened");
        } catch (err) {
            console.error(err);
            toast.error("Failed to reopen evaluation");
        } finally {
            processingAction = false;
        }
    }
</script>

<div class="flex flex-col gap-6 max-w-4xl mx-auto pb-20">
    <div class="flex items-center justify-between gap-4">
        <div class="flex items-center gap-4">
            <Button
                variant="ghost"
                size="icon"
                href="/evaluations"
                onclick={() => goto("/evaluations")}
            >
                <ArrowLeft class="h-4 w-4" />
            </Button>
            <div>
                <h1 class="text-lg font-semibold md:text-2xl">
                    {mode === "view" ? "View Evaluation" : "Edit Evaluation"}
                </h1>
                {#if evaluation}
                    <Badge
                        variant={evaluation.status === "COMPLETED"
                            ? "default"
                            : "outline"}
                    >
                        {evaluation.status}
                    </Badge>
                {/if}
            </div>
        </div>
        {#if evaluation && mode === "view" && evaluation.status !== "INVALIDATED"}
            <div class="flex gap-2">
                {#if evaluation.status !== "FLAGGED"}
                    <Button
                        variant="outline"
                        size="sm"
                        onclick={handleFlag}
                        disabled={processingAction}
                    >
                        <Flag class="mr-2 h-4 w-4" /> Flag
                    </Button>
                {/if}
                <Button
                    variant="destructive"
                    size="sm"
                    onclick={handleInvalidate}
                    disabled={processingAction}
                >
                    <Ban class="mr-2 h-4 w-4" /> Invalidate
                </Button>
                <Button
                    variant="outline"
                    size="sm"
                    onclick={handleReopen}
                    disabled={processingAction}
                >
                    Reopen
                </Button>
            </div>
        {/if}
    </div>

    {#if isLoading}
        <div class="flex justify-center p-12">
            <Loader2 class="h-8 w-8 animate-spin text-primary" />
        </div>
    {:else if error}
        <div class="rounded-md bg-red-50 p-4 text-sm text-red-700">
            {error}
        </div>
    {:else}
        {#if adminDetail}
            <div class="rounded-md border bg-muted/20 p-3 text-xs overflow-auto">
                <p class="font-medium mb-1">Admin Submission Detail</p>
                <DataView data={adminDetail} />
            </div>
        {/if}
        <EvaluationForm
            {template}
            {initialAnswers}
            onSubmit={handleSubmit}
            {isSubmitting}
            {mode}
        />
    {/if}
</div>
