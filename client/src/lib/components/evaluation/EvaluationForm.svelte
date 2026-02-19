<script lang="ts">
    import { Button } from "$lib/components/ui/button/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import * as RadioGroup from "$lib/components/ui/radio-group/index.js";
    import { Checkbox } from "$lib/components/ui/checkbox/index.js";
    import { Loader2, Save } from "@lucide/svelte";

    let {
        template,
        initialAnswers = {},
        onSubmit,
        isSubmitting,
        mode = "create",
    } = $props();

    // Deep copy initial answers or initialize
    let answers = $state<Record<string, any>>({});

    function normalizeAnswer(questionId: string, incoming?: any) {
        return {
            questionId,
            value: incoming?.value ?? null,
            selectedOptions: Array.isArray(incoming?.selectedOptions)
                ? [...incoming.selectedOptions]
                : [],
            textResponse: incoming?.textResponse ?? "",
            metadata: incoming?.metadata ?? {},
        };
    }

    $effect(() => {
        if (template) {
            template.sections.forEach((section: any) => {
                section.questions.forEach((q: any) => {
                    if (initialAnswers[q.id]) {
                        answers[q.id] = normalizeAnswer(q.id, initialAnswers[q.id]);
                    } else {
                        answers[q.id] = normalizeAnswer(q.id);
                    }
                });
            });
        }
    });

    function toggleMultiChoice(questionId: string, option: string, checked: boolean) {
        if (!answers[questionId]) {
            answers[questionId] = normalizeAnswer(questionId);
        }
        const current = answers[questionId]?.selectedOptions ?? [];
        if (checked) {
            if (!current.includes(option)) {
                answers[questionId].selectedOptions = [...current, option];
            }
            return;
        }
        answers[questionId].selectedOptions = current.filter((v: string) => v !== option);
    }

    function ensureAnswer(questionId: string) {
        if (!answers[questionId]) {
            answers[questionId] = normalizeAnswer(questionId);
        }
        return answers[questionId];
    }

    function getAnswer(questionId: string) {
        return answers[questionId] ?? normalizeAnswer(questionId);
    }

    function handleSubmit() {
        const answersList = Object.values(answers);
        onSubmit(answersList);
    }
</script>

{#if template}
    <div class="space-y-6 pb-20">
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

                            {#if question.type === "OPEN_TEXT"}
                                <Textarea
                                    value={getAnswer(question.id).textResponse}
                                    oninput={(e) =>
                                        (ensureAnswer(question.id).textResponse =
                                            (e.currentTarget as HTMLTextAreaElement).value)}
                                    placeholder="Type your answer here..."
                                    disabled={mode === "view"}
                                />
                            {:else if question.type === "SINGLE_CHOICE"}
                                <RadioGroup.Root
                                    value={getAnswer(question.id).selectedOptions?.[0] ?? ""}
                                    disabled={mode === "view"}
                                    onValueChange={(v) =>
                                        (ensureAnswer(question.id).selectedOptions =
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
                                            disabled={mode === "view"}
                                            class={`h-10 w-10 rounded-full border flex items-center justify-center transition-colors 
                                                ${getAnswer(question.id).value == rating ? "bg-primary text-primary-foreground border-primary" : "bg-background hover:bg-muted"}
                                                ${mode === "view" ? "cursor-not-allowed opacity-80" : ""}
                                            `}
                                            onclick={() => {
                                                if (mode !== "view")
                                                    ensureAnswer(question.id).value =
                                                        rating;
                                            }}
                                        >
                                            {rating}
                                        </button>
                                    {/each}
                                </div>
                            {:else if question.type === "BOOLEAN"}
                                <RadioGroup.Root
                                    value={getAnswer(question.id).value?.toString() ?? ""}
                                    disabled={mode === "view"}
                                    onValueChange={(v) =>
                                        (ensureAnswer(question.id).value = v)}
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
                                                disabled={mode === "view"}
                                                checked={getAnswer(question.id).selectedOptions?.includes(option) ?? false}
                                                onCheckedChange={(v) => {
                                                    if (mode !== "view") {
                                                        toggleMultiChoice(
                                                            question.id,
                                                            option,
                                                            v === true,
                                                        );
                                                    }
                                                }}
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

        {#if mode !== "view"}
            <div
                class="fixed bottom-0 left-0 right-0 border-t bg-background p-4 md:left-[220px] lg:left-[280px]"
            >
                <div class="flex justify-end gap-4 max-w-4xl mx-auto">
                    <Button variant="ghost" href="/evaluations">Cancel</Button>
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
{/if}
