<script lang="ts">
    import DataView from "./data-view.svelte";

    let { data, compact = false } = $props<{ data: any; compact?: boolean }>();

    function isPrimitive(value: any) {
        return value == null || ["string", "number", "boolean"].includes(typeof value);
    }
</script>

{#if data == null}
    <span class="text-sm text-muted-foreground">-</span>
{:else if isPrimitive(data)}
    <span class="text-sm">{String(data)}</span>
{:else if Array.isArray(data)}
    {#if data.length === 0}
        <span class="text-sm text-muted-foreground">No items</span>
    {:else}
        <div class={compact ? "space-y-1" : "space-y-2"}>
            {#each data as item, index}
                <div class="rounded-md border p-2">
                    <p class="text-[11px] text-muted-foreground">Item {index + 1}</p>
                    <DataView data={item} compact={true} />
                </div>
            {/each}
        </div>
    {/if}
{:else}
    <div class={compact ? "space-y-1" : "space-y-2"}>
        {#each Object.entries(data) as [key, value]}
            <div class="rounded-md border p-2">
                <p class="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">{key}</p>
                <DataView data={value} compact={true} />
            </div>
        {/each}
    </div>
{/if}
