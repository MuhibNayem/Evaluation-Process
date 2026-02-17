<script lang="ts">
    import { page } from "$app/stores";
    import { goto } from "$app/navigation";
    import api from "$lib/api.js";
    import { onMount } from "svelte";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import * as Select from "$lib/components/ui/select/index.js";
    import { Switch } from "$lib/components/ui/switch/index.js";
    import {
        Loader2,
        ArrowLeft,
        Trash2,
        Plus,
        GripVertical,
        Save,
    } from "@lucide/svelte";

    let templateId = $page.params.id;
    let isSubmitting = $state(false);
    let isLoading = $state(true);
    let error = $state<string | null>(null);

    // Template State
    let template = $state<any>({
        name: "",
        description: "",
        category: "",
        scoringMethod: "WEIGHTED_AVERAGE",
        sections: [],
    });

    onMount(async () => {
        try {
            const res = await api.get(`/templates/${templateId}`);
            const data = res.data;
            // Map backend data to frontend state structure if needed
            // Ensure sections/questions have appropriate structure
            template = {
                ...data,
                sections: data.sections.map((s: any) => ({
                    ...s,
                    id: s.id, // Keep ID for potential updates
                    questions: s.questions.map((q: any) => ({
                        ...q,
                        id: q.id,
                        // Ensure options is an array
                        options: q.options || [],
                    })),
                })),
            };
        } catch (err: any) {
            console.error(err);
            error = "Failed to load template.";
        } finally {
            isLoading = false;
        }
    });

    function addSection() {
        template.sections.push({
            // No ID for new sections, backend will generate
            title: "",
            description: "",
            weight: 1,
            questions: [],
        });
    }

    function removeSection(index: number) {
        template.sections.splice(index, 1);
    }

    function addQuestion(sectionIndex: number) {
        template.sections[sectionIndex].questions.push({
            text: "",
            type: "SINGLE_CHOICE",
            required: true,
            weight: 1,
            options: [],
        });
    }

    function removeQuestion(sectionIndex: number, questionIndex: number) {
        template.sections[sectionIndex].questions.splice(questionIndex, 1);
    }

    function addOption(sectionIndex: number, questionIndex: number) {
        template.sections[sectionIndex].questions[questionIndex].options.push(
            "",
        );
    }

    function removeOption(
        sectionIndex: number,
        questionIndex: number,
        optionIndex: number,
    ) {
        template.sections[sectionIndex].questions[questionIndex].options.splice(
            optionIndex,
            1,
        );
    }

    async function handleSubmit(e: Event) {
        e.preventDefault();
        isSubmitting = true;
        error = null;

        try {
            const payload = {
                name: template.name,
                description: template.description,
                category: template.category,
                scoringMethod: template.scoringMethod,
                customFormula: template.customFormula,
                sections: template.sections.map((s: any, sIdx: number) => ({
                    id: s.id, // Send ID if exists
                    title: s.title,
                    description: s.description,
                    weight: s.weight,
                    orderIndex: sIdx,
                    questions: s.questions.map((q: any, qIdx: number) => ({
                        id: q.id, // Send ID if exists
                        text: q.text,
                        type: q.type,
                        required: q.required,
                        weight: q.weight,
                        orderIndex: qIdx,
                        options: ["SINGLE_CHOICE", "MULTIPLE_CHOICE"].includes(
                            q.type,
                        )
                            ? q.options
                            : [],
                    })),
                })),
            };

            await api.put(`/templates/${templateId}`, payload);
            goto(`/templates/${templateId}`); // Go to View page
        } catch (err: any) {
            console.error("Failed to update template:", err);
            error = err.response?.data?.detail || "Failed to update template.";
        } finally {
            isSubmitting = false;
        }
    }
</script>

<div class="flex flex-col gap-6 pb-20">
    <div class="flex items-center gap-4">
        <Button
            variant="ghost"
            size="icon"
            href={`/templates/${templateId}`}
            onclick={() => goto(`/templates/${templateId}`)}
        >
            <ArrowLeft class="h-4 w-4" />
        </Button>
        <h1 class="text-lg font-semibold md:text-2xl">
            Edit {template.name || "Template"}
        </h1>
    </div>

    {#if isLoading}
        <div class="flex justify-center py-20">
            <Loader2 class="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
    {:else}
        <form onsubmit={handleSubmit} class="grid gap-6">
            {#if error}
                <div
                    class="rounded-md bg-red-50 p-4 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-400"
                >
                    {error}
                </div>
            {/if}

            <Card.Root>
                <Card.Header>
                    <Card.Title>Template Details</Card.Title>
                </Card.Header>
                <Card.Content class="grid gap-4">
                    <div class="grid gap-2">
                        <Label for="name">Template Name</Label>
                        <Input id="name" bind:value={template.name} required />
                    </div>
                    <div class="grid gap-2">
                        <Label for="description">Description</Label>
                        <Textarea
                            id="description"
                            bind:value={template.description}
                        />
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div class="grid gap-2">
                            <Label for="category">Category</Label>
                            <Input
                                id="category"
                                bind:value={template.category}
                            />
                        </div>
                        <div class="grid gap-2">
                            <Label for="scoring">Scoring Method</Label>
                            <Select.Root
                                type="single"
                                value={template.scoringMethod}
                                onValueChange={(v) =>
                                    (template.scoringMethod = v)}
                            >
                                <Select.Trigger
                                    >{template.scoringMethod}</Select.Trigger
                                >
                                <Select.Content>
                                    <Select.Item value="WEIGHTED_AVERAGE"
                                        >Weighted Average</Select.Item
                                    >
                                    <Select.Item value="SIMPLE_AVERAGE"
                                        >Simple Average</Select.Item
                                    >
                                </Select.Content>
                            </Select.Root>
                        </div>
                    </div>
                </Card.Content>
            </Card.Root>

            <div class="flex items-center justify-between">
                <h2 class="text-xl font-semibold">Sections</h2>
                <Button type="button" onclick={addSection} variant="outline">
                    <Plus class="mr-2 h-4 w-4" /> Add Section
                </Button>
            </div>

            {#each template.sections as section, sIdx}
                <Card.Root class="border-l-4 border-l-primary/50">
                    <Card.Header class="flex flex-row items-center gap-4 py-4">
                        <GripVertical
                            class="h-5 w-5 text-muted-foreground cursor-move"
                        />
                        <div class="flex-1 grid gap-2">
                            <Input
                                bind:value={section.title}
                                placeholder="Section Title"
                                class="font-semibold text-lg"
                            />
                            <Input
                                bind:value={section.description}
                                placeholder="Description"
                                class="text-sm text-muted-foreground h-8"
                            />
                        </div>
                        <div class="flex items-center gap-2">
                            <Label class="whitespace-nowrap">Weight:</Label>
                            <Input
                                type="number"
                                bind:value={section.weight}
                                class="w-20"
                                min="0"
                                step="0.1"
                            />
                        </div>
                        <Button
                            variant="ghost"
                            size="icon"
                            class="text-destructive"
                            onclick={() => removeSection(sIdx)}
                        >
                            <Trash2 class="h-4 w-4" />
                        </Button>
                    </Card.Header>
                    <Card.Content class="pl-12 pr-4 pb-4">
                        <div class="space-y-4">
                            {#each section.questions as question, qIdx}
                                <div
                                    class="rounded-md border p-4 bg-muted/20 hover:bg-muted/40 transition-colors"
                                >
                                    <div class="flex gap-4 items-start">
                                        <div class="flex-1 grid gap-3">
                                            <Input
                                                bind:value={question.text}
                                                placeholder="Question Text"
                                            />
                                            <div
                                                class="flex items-center gap-4"
                                            >
                                                <Select.Root
                                                    type="single"
                                                    value={question.type}
                                                    onValueChange={(v) =>
                                                        (question.type = v)}
                                                >
                                                    <Select.Trigger
                                                        class="w-[180px]"
                                                        >{question.type}</Select.Trigger
                                                    >
                                                    <Select.Content>
                                                        <Select.Item
                                                            value="SINGLE_CHOICE"
                                                            >Single Choice</Select.Item
                                                        >
                                                        <Select.Item
                                                            value="MULTIPLE_CHOICE"
                                                            >Multiple Choice</Select.Item
                                                        >
                                                        <Select.Item
                                                            value="OPEN_TEXT"
                                                            >Open Text</Select.Item
                                                        >
                                                        <Select.Item
                                                            value="RATING"
                                                            >Rating (1-5)</Select.Item
                                                        >
                                                        <Select.Item
                                                            value="BOOLEAN"
                                                            >Yes/No</Select.Item
                                                        >
                                                    </Select.Content>
                                                </Select.Root>
                                                <div
                                                    class="flex items-center gap-2"
                                                >
                                                    <Label class="text-xs"
                                                        >Weight:</Label
                                                    >
                                                    <Input
                                                        type="number"
                                                        bind:value={
                                                            question.weight
                                                        }
                                                        class="w-16 h-8"
                                                        min="0"
                                                        step="0.1"
                                                    />
                                                </div>
                                                <div
                                                    class="flex items-center gap-2"
                                                >
                                                    <Switch
                                                        checked={question.required}
                                                        onCheckedChange={(v) =>
                                                            (question.required =
                                                                v)}
                                                    />
                                                    <Label class="text-xs"
                                                        >Required</Label
                                                    >
                                                </div>
                                            </div>

                                            {#if ["SINGLE_CHOICE", "MULTIPLE_CHOICE"].includes(question.type)}
                                                <div
                                                    class="pl-4 border-l-2 space-y-2 mt-2"
                                                >
                                                    <Label
                                                        class="text-xs text-muted-foreground"
                                                        >Options</Label
                                                    >
                                                    {#each question.options as option, oIdx}
                                                        <div
                                                            class="flex items-center gap-2"
                                                        >
                                                            <div
                                                                class="h-2 w-2 rounded-full bg-muted-foreground/30"
                                                            ></div>
                                                            <Input
                                                                bind:value={
                                                                    question
                                                                        .options[
                                                                        oIdx
                                                                    ]
                                                                }
                                                                placeholder={`Option ${oIdx + 1}`}
                                                                class="h-8"
                                                            />
                                                            <Button
                                                                variant="ghost"
                                                                size="icon"
                                                                class="h-6 w-6"
                                                                onclick={() =>
                                                                    removeOption(
                                                                        sIdx,
                                                                        qIdx,
                                                                        oIdx,
                                                                    )}
                                                            >
                                                                <Trash2
                                                                    class="h-3 w-3"
                                                                />
                                                            </Button>
                                                        </div>
                                                    {/each}
                                                    <Button
                                                        variant="link"
                                                        size="sm"
                                                        class="h-6 px-0"
                                                        onclick={() =>
                                                            addOption(
                                                                sIdx,
                                                                qIdx,
                                                            )}
                                                    >
                                                        + Add Option
                                                    </Button>
                                                </div>
                                            {/if}
                                        </div>
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            class="text-muted-foreground hover:text-destructive"
                                            onclick={() =>
                                                removeQuestion(sIdx, qIdx)}
                                        >
                                            <Trash2 class="h-4 w-4" />
                                        </Button>
                                    </div>
                                </div>
                            {/each}
                            <Button
                                variant="secondary"
                                size="sm"
                                class="w-full dashed-border"
                                onclick={() => addQuestion(sIdx)}
                            >
                                <Plus class="mr-2 h-3 w-3" /> Add Question
                            </Button>
                        </div>
                    </Card.Content>
                </Card.Root>
            {/each}

            <div
                class="fixed bottom-0 left-0 right-0 border-t bg-background p-4 md:left-[220px] lg:left-[280px]"
            >
                <div class="flex justify-end gap-4 max-w-5xl mx-auto">
                    <Button
                        variant="ghost"
                        type="button"
                        onclick={() => goto(`/templates/${templateId}`)}
                        >Cancel</Button
                    >
                    <Button type="submit" disabled={isSubmitting}>
                        {#if isSubmitting}
                            <Loader2 class="mr-2 h-4 w-4 animate-spin" /> Saving...
                        {:else}
                            <Save class="mr-2 h-4 w-4" /> Save Changes
                        {/if}
                    </Button>
                </div>
            </div>
        </form>
    {/if}
</div>
