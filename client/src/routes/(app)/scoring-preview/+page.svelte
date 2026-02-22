<script lang="ts">
    import { onMount } from "svelte";
    import api from "$lib/api.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Textarea } from "$lib/components/ui/textarea/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import DataView from "$lib/components/data-view.svelte";
    import { Loader2 } from "@lucide/svelte";
    import { toast } from "svelte-sonner";

    let templates = $state<any[]>([]);
    let loadingTemplates = $state(false);
    let running = $state(false);
    let result = $state<any>(null);

    let form = $state({
        templateId: "",
        scoringMethodOverride: "",
        customFormulaOverride: "",
        answersText: '[{"questionId":"q1","value":8}]',
    });

    async function loadTemplates() {
        loadingTemplates = true;
        try {
            const res = await api.get("/templates", { params: { page: 0, size: 200 } });
            templates = res.data;
            if (!form.templateId && templates.length) form.templateId = templates[0].id;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || "Failed to load templates");
        } finally {
            loadingTemplates = false;
        }
    }

    async function preview() {
        if (!form.templateId) return;
        running = true;
        try {
            const answers = JSON.parse(form.answersText || "[]");
            const payload: Record<string, unknown> = {
                templateId: form.templateId,
                answers,
            };
            if (form.scoringMethodOverride.trim()) payload.scoringMethodOverride = form.scoringMethodOverride.trim();
            if (form.customFormulaOverride.trim()) payload.customFormulaOverride = form.customFormulaOverride;

            const res = await api.post("/scoring/preview", payload);
            result = res.data;
        } catch (err: any) {
            toast.error(err.response?.data?.detail || err.message || "Failed to preview scoring");
        } finally {
            running = false;
        }
    }

    onMount(loadTemplates);
</script>

<div class="space-y-6 pb-20">
    <div>
        <h1 class="text-3xl font-bold tracking-tight">Scoring Preview</h1>
        <p class="text-muted-foreground">Admin endpoint: POST /api/v1/scoring/preview.</p>
    </div>

    <Card.Root>
        <Card.Header>
            <Card.Title>Preview Request</Card.Title>
        </Card.Header>
        <Card.Content class="grid gap-3">
            {#if loadingTemplates}
                <p class="text-sm text-muted-foreground flex items-center gap-2"><Loader2 class="h-4 w-4 animate-spin" /> Loading templates...</p>
            {/if}
            <Input bind:value={form.templateId} placeholder="templateId" />
            <Input bind:value={form.scoringMethodOverride} placeholder="scoringMethodOverride (optional)" />
            <Input bind:value={form.customFormulaOverride} placeholder="customFormulaOverride (optional)" />
            <Textarea bind:value={form.answersText} rows={8} placeholder='answers JSON array' />
            <Button onclick={preview} disabled={running || !form.templateId}>
                {#if running}<Loader2 class="mr-2 h-4 w-4 animate-spin" />{/if}
                Run Preview
            </Button>
        </Card.Content>
    </Card.Root>

    {#if result}
        <Card.Root>
            <Card.Header>
                <Card.Title>Preview Result</Card.Title>
                <Card.Description>Method: {result.scoringMethod} | Total: {result.totalScore}</Card.Description>
            </Card.Header>
            <Card.Content class="space-y-3">
                {#if result.sections?.length}
                    <div class="grid gap-3 md:grid-cols-2">
                        {#each result.sections as s}
                            <div class="rounded-md border p-3 text-sm">
                                <p class="font-medium">{s.sectionTitle} ({s.sectionId})</p>
                                <p>Score: {s.score} / {s.maxPossible}</p>
                                <p>Answered: {s.answeredQuestions}/{s.totalQuestions}</p>
                            </div>
                        {/each}
                    </div>
                {/if}
                <div class="rounded-md border bg-muted/30 p-3">
                    <DataView data={result} />
                </div>
            </Card.Content>
        </Card.Root>
    {/if}
</div>
