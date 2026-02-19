<script lang="ts">
    import { page } from "$app/stores";
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import { goto } from "$app/navigation";
    import { Button } from "$lib/components/ui/button/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import * as RadioGroup from "$lib/components/ui/radio-group/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import { Loader2, ArrowLeft, Save } from "@lucide/svelte";

    // We get assignmentId from the URL
    // @ts-ignore
    let assignmentId = $derived($page.params.id);

    let isLoading = $state(true);
    let isSubmitting = $state(false);
    let error = $state<string | null>(null);

    let assignment = $state<any>(null);
    let campaign = $state<any>(null);
    let template = $state<any>(null);

    // Form State: Map questionId -> Answer
    let answers = $state<Record<string, any>>({});

    async function fetchData() {
        isLoading = true;
        try {
            // 1. Get Assignment details (Re-fetch list for simplicity)
            const assignmentsRes = await api.get("/campaigns/assignments/me");
            assignment = assignmentsRes.data.find(
                (a: any) => a.id === assignmentId,
            );

            if (!assignment) {
                error = "Assignment not found.";
                return;
            }

            // 2. Get Campaign to identify Template
            const campaignRes = await api.get(
                `/campaigns/${assignment.campaignId}`,
            );
            campaign = campaignRes.data;

            // 3. Get Template
            const templateRes = await api.get(
                `/templates/${campaign.templateId}`,
            );
            template = templateRes.data;

            // Initialize answers structure
            template.sections.forEach((section: any) => {
                section.questions.forEach((q: any) => {
                    answers[q.id] = {
                        questionId: q.id,
                        value: null,
                        selectedOptions: [],
                        textResponse: "",
                    };
                });
            });
        } catch (err) {
            console.error(err);
            error = "Failed to load evaluation data.";
        } finally {
            isLoading = false;
        }
    }

    onMount(() => {
        if (assignmentId) fetchData();
    });

    function toggleMultiChoice(questionId: string, option: string, checked: boolean) {
        const current = answers[questionId]?.selectedOptions ?? [];
        if (checked) {
            if (!current.includes(option)) {
                answers[questionId].selectedOptions = [...current, option];
            }
            return;
        }
        answers[questionId].selectedOptions = current.filter((v: string) => v !== option);
    }

    async function handleSubmit() {
        isSubmitting = true;
        try {
            // Transform answers object to list
            const answersList = Object.values(answers).map((a: any) => {
                const selectedOptions = a.selectedOptions ?? [];
                const normalizedValue =
                    a.value ?? (selectedOptions.length === 1 ? selectedOptions[0] : null);
                // Ensure correct format for backend
                return {
                    questionId: a.questionId,
                    value: normalizedValue != null ? normalizedValue.toString() : null,
                    selectedOptions,
                    textResponse: a.textResponse,
                    metadata: {},
                };
            });

            const payload = {
                campaignId: assignment.campaignId,
                assignmentId: assignment.id,
                // Must be non-empty to satisfy request validation; backend resolves authenticated user.
                evaluatorId: "context-user",
                evaluateeId: assignment.evaluateeId,
                templateId: template.id,
                answers: answersList,
            };

            await api.post("/evaluations", payload);
            goto("/evaluations");
        } catch (err: any) {
            console.error(err);
            error = "Failed to submit evaluation.";
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="flex flex-col gap-6 max-w-4xl mx-auto pb-20">
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
            <h1 class="text-lg font-semibold md:text-2xl">New Evaluation</h1>
            {#if campaign}
                <p class="text-sm text-muted-foreground">{campaign.name}</p>
            {/if}
        </div>
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
        {#each template.sections as section}
            <Card.Root>
                <Card.Header>
                    <Card.Title>{section.title}</Card.Title>
                    {#if section.description}
                        <Card.Description
                            >{section.description}</Card.Description
                        >
                    {/if}
                </Card.Header>
                <Card.Content class="space-y-6">
                    {#each section.questions as question}
                        <div class="space-y-3">
                            <Label class="text-base font-medium">
                                {question.text}
                                {#if question.required}
                                    <span class="text-red-500 ml-1">*</span>
                                {/if}
                            </Label>

                            <!-- Render Input based on Type -->
                            {#if question.type === "OPEN_TEXT"}
                                <Textarea
                                    bind:value={
                                        answers[question.id].textResponse
                                    }
                                    placeholder="Type your answer here..."
                                />
                            {:else if question.type === "SINGLE_CHOICE"}
                                <RadioGroup.Root
                                    value={answers[question.id]?.selectedOptions?.[0] ?? ""}
                                    onValueChange={(v) =>
                                        (answers[question.id].selectedOptions =
                                            [v])}
                                >
                                    {#each question.options as option}
                                        <div
                                            class="flex items-center space-x-2"
                                        >
                                            <RadioGroup.Item
                                                value={option}
                                                id={`${question.id}-${option}`}
                                            />
                                            <Label
                                                for={`${question.id}-${option}`}
                                                >{option}</Label
                                            >
                                        </div>
                                    {/each}
                                </RadioGroup.Root>
                            {:else if question.type === "RATING" || question.type === "NUMERIC_RATING"}
                                <div class="flex items-center gap-2">
                                    {#each [1, 2, 3, 4, 5] as rating}
                                        <button
                                            type="button"
                                            class={`h-10 w-10 rounded-full border flex items-center justify-center transition-colors ${answers[question.id].value == rating ? "bg-primary text-primary-foreground border-primary" : "bg-background hover:bg-muted"}`}
                                            onclick={() =>
                                                (answers[question.id].value =
                                                    rating)}
                                        >
                                            {rating}
                                        </button>
                                    {/each}
                                </div>
                            {:else if question.type === "BOOLEAN"}
                                <RadioGroup.Root
                                    value={answers[question.id]?.value?.toString() ?? ""}
                                    onValueChange={(v) =>
                                        (answers[question.id].value = v)}
                                >
                                    <div class="flex items-center space-x-4">
                                        <div
                                            class="flex items-center space-x-2"
                                        >
                                            <RadioGroup.Item
                                                value="true"
                                                id={`${question.id}-yes`}
                                            />
                                            <Label for={`${question.id}-yes`}
                                                >Yes</Label
                                            >
                                        </div>
                                        <div
                                            class="flex items-center space-x-2"
                                        >
                                            <RadioGroup.Item
                                                value="false"
                                                id={`${question.id}-no`}
                                            />
                                            <Label for={`${question.id}-no`}
                                                >No</Label
                                            >
                                        </div>
                                    </div>
                                </RadioGroup.Root>
                            {:else if question.type === "MULTIPLE_CHOICE"}
                                <div class="space-y-2">
                                    {#each question.options as option}
                                        <div class="flex items-center space-x-2">
                                            <Checkbox
                                                id={`${question.id}-${option}`}
                                                checked={answers[question.id]?.selectedOptions?.includes(option) ?? false}
                                                onCheckedChange={(v) =>
                                                    toggleMultiChoice(
                                                        question.id,
                                                        option,
                                                        v === true,
                                                    )}
                                            />
                                            <Label for={`${question.id}-${option}`}>{option}</Label>
                                        </div>
                                    {/each}
                                </div>
                            {/if}
                        </div>
                    {/each}
                </Card.Content>
            </Card.Root>
        {/each}

        <div
            class="fixed bottom-0 left-0 right-0 border-t bg-background p-4 md:left-[220px] lg:left-[280px]"
        >
            <div class="flex justify-end gap-4 max-w-4xl mx-auto">
                <Button variant="ghost" onclick={() => goto("/evaluations")}
                    >Cancel</Button
                >
                <Button onclick={handleSubmit} disabled={isSubmitting}>
                    {#if isSubmitting}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                        Submitting...
                    {:else}
                        <Save class="mr-2 h-4 w-4" />
                        Submit Evaluation
                    {/if}
                </Button>
            </div>
        </div>
    {/if}
</div>
