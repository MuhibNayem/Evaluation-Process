<script lang="ts">
    import { auth } from "$lib/stores/auth.svelte.js";
    import { goto } from "$app/navigation";
    import * as DropdownMenu from "$lib/components/ui/dropdown-menu/index.js";
    import * as Avatar from "$lib/components/ui/avatar/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { LogOut, User } from "@lucide/svelte";

    function handleLogout() {
        auth.logout();
        goto("/login");
    }

    let userInitials = $derived(
        auth.user?.username?.substring(0, 2).toUpperCase() || "U",
    );
</script>

<header
    class="sticky top-0 z-30 flex h-16 items-center border-b bg-background px-6 shadow-sm"
>
    <div class="mr-4 hidden md:flex">
        <a href="/" class="flex items-center gap-2 font-semibold">
            <span class="text-lg">Evaluation Service</span>
        </a>
    </div>

    <div class="ml-auto flex items-center space-x-4">
        <DropdownMenu.Root>
            <DropdownMenu.Trigger>
                <Button variant="ghost" class="relative h-8 w-8 rounded-full">
                    <Avatar.Root class="h-8 w-8">
                        <Avatar.Fallback>{userInitials}</Avatar.Fallback>
                    </Avatar.Root>
                </Button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Content class="w-56" align="end">
                <DropdownMenu.Label class="font-normal">
                    <div class="flex flex-col space-y-1">
                        <p class="text-sm font-medium leading-none">
                            {auth.user?.username}
                        </p>
                        <p class="text-xs leading-none text-muted-foreground">
                            User
                        </p>
                    </div>
                </DropdownMenu.Label>
                <DropdownMenu.Separator />
                <DropdownMenu.Item onSelect={handleLogout}>
                    <LogOut class="mr-2 h-4 w-4" />
                    <span>Log out</span>
                </DropdownMenu.Item>
            </DropdownMenu.Content>
        </DropdownMenu.Root>
    </div>
</header>
