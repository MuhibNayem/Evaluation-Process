<script lang="ts">
    import {
        DateFormatter,
        type DateValue,
        getLocalTimeZone,
        parseDate,
        today,
    } from "@internationalized/date";
    import { Calendar as CalendarIcon } from "@lucide/svelte";
    import { cn } from "$lib/utils.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Calendar } from "$lib/components/ui/calendar/index.js";
    import * as Popover from "$lib/components/ui/popover/index.js";

    let { value = $bindable() }: { value: string | undefined } = $props();

    const df = new DateFormatter("en-US", {
        dateStyle: "long",
    });

    let dateValue = $state<DateValue | undefined>(
        value ? parseDate(value.split("T")[0]) : undefined,
    );

    $effect(() => {
        if (dateValue) {
            value = dateValue.toString();
        } else {
            value = undefined;
        }
    });

    // Handle updates from parent if value changes externally (not by Calendar)
    // This requires a separate effect or careful binding.
    // Simplifying: If value is passed, parse it.
    // But modifying dateValue updates value via effect.
    // If parent updates value, dateValue needs update.
    // We can use a derived state or a synced effect.
    $effect(() => {
        if (
            value &&
            (!dateValue || dateValue.toString() !== value.split("T")[0])
        ) {
            try {
                dateValue = parseDate(value.split("T")[0]);
            } catch (e) {
                // ignore
            }
        }
    });
</script>

<Popover.Root>
    <Popover.Trigger>
        {#snippet child({ props }: { props: Record<string, any> })}
            <Button
                variant="outline"
                class={cn(
                    "w-[280px] justify-start text-left font-normal",
                    !dateValue && "text-muted-foreground",
                )}
                {...props}
            >
                <CalendarIcon class="mr-2 h-4 w-4" />
                {dateValue
                    ? df.format(dateValue.toDate(getLocalTimeZone()))
                    : "Pick a date"}
            </Button>
        {/snippet}
    </Popover.Trigger>
    <Popover.Content class="w-auto p-0">
        <Calendar type="single" bind:value={dateValue} />
    </Popover.Content>
</Popover.Root>
