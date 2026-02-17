<script lang="ts">
    import { auth } from "$lib/stores/auth.svelte.js";
    import { goto } from "$app/navigation";
    import { browser } from "$app/environment";
    import Header from "$lib/components/layout/Header.svelte";
    import Sidebar from "$lib/components/layout/Sidebar.svelte";

    let { children } = $props();

    $effect(() => {
        if (browser && !auth.isAuthenticated) {
            goto("/login");
        }
    });
</script>

{#if auth.isAuthenticated}
    <div
        class="grid min-h-screen w-full md:grid-cols-[220px_1fr] lg:grid-cols-[280px_1fr]"
    >
        <Sidebar />
        <div class="flex flex-col">
            <Header />
            <main class="flex flex-1 flex-col gap-4 p-4 lg:gap-6 lg:p-6">
                {@render children()}
            </main>
        </div>
    </div>
{:else}
    <div class="flex h-screen items-center justify-center">
        <p>Loading...</p>
    </div>
{/if}
