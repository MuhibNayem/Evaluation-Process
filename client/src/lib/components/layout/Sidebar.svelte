<script lang="ts">
    import { page } from "$app/stores";
    import {
        LayoutDashboard,
        Users,
        FileText,
        CheckSquare,
    } from "@lucide/svelte";
    import { cn } from "$lib/utils.js";

    // Use $derived to reactively get the current path
    let currentPath = $derived($page.url.pathname);

    const links = [
        { href: "/", label: "Dashboard", icon: LayoutDashboard },
        { href: "/campaigns", label: "Campaigns", icon: Users },
        { href: "/templates", label: "Templates", icon: FileText },
        { href: "/evaluations", label: "My Evaluations", icon: CheckSquare },
    ];
</script>

<aside class="hidden w-64 flex-col border-r bg-muted/40 md:flex">
    <div class="flex h-full flex-col gap-2">
        <div class="flex-1 overflow-auto py-2">
            <nav class="grid items-start px-2 text-sm font-medium lg:px-4">
                {#each links as link}
                    <a
                        href={link.href}
                        class={cn(
                            "flex items-center gap-3 rounded-lg px-3 py-2 transition-all hover:text-primary",
                            currentPath === link.href ||
                                (link.href !== "/" &&
                                    currentPath.startsWith(link.href))
                                ? "bg-muted text-primary"
                                : "text-muted-foreground",
                        )}
                    >
                        <link.icon class="h-4 w-4" />
                        {link.label}
                    </a>
                {/each}
            </nav>
        </div>
    </div>
</aside>
