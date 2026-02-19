<script lang="ts">
    import * as Popover from "$lib/components/ui/popover/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { CircleHelp } from "@lucide/svelte";

    let {
        title = "Field Help",
        what = "",
        why = "",
        how = "",
        example = "",
    }: {
        title?: string;
        what: string;
        why: string;
        how: string;
        example: string;
    } = $props();
</script>

<Popover.Root>
    <Popover.Trigger>
        <Button type="button" variant="ghost" size="icon" class="h-6 w-6">
            <CircleHelp class="h-4 w-4" />
        </Button>
    </Popover.Trigger>
    <Popover.Content class="w-96 space-y-2">
        <h4 class="text-sm font-semibold">{title}</h4>
        <p class="text-xs"><strong>What:</strong> {what}</p>
        <p class="text-xs"><strong>Why:</strong> {why}</p>
        <p class="text-xs"><strong>How:</strong> {how}</p>
        <p class="text-xs"><strong>Example:</strong> <code>{example}</code></p>
    </Popover.Content>
</Popover.Root>
