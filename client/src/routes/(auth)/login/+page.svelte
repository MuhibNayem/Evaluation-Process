<script lang="ts">
    import { goto } from "$app/navigation";
    import { auth } from "$lib/stores/auth.svelte.js";
    import api from "$lib/api.js";
    import * as Card from "$lib/components/ui/card/index.js";
    import { Button } from "$lib/components/ui/button/index.js";
    import { Input } from "$lib/components/ui/input/index.js";
    import { Label } from "$lib/components/ui/label/index.js";
    import { Loader2 } from "@lucide/svelte";
    import { SECURITY_DEV_MODE } from "$lib/config.js";

    let username = $state("");
    let password = $state("");
    let isLoading = $state(false);
    let error = $state<string | null>(null);

    async function handleLogin(e: Event) {
        e.preventDefault();
        isLoading = true;
        error = null;

        try {
            const response = await api.post("/auth/login", {
                username,
                password,
            });
            const { token } = response.data;
            auth.setToken(token);
            // Decode token to get user info if needed, or fetch user profile
            // For now, just set a simple user object
            auth.setUser({ username });
            goto("/");
        } catch (err: any) {
            console.error(err);
            error =
                err.response?.status === 401
                    ? "Invalid credentials"
                    : "An error occurred. Please try again.";
        } finally {
            isLoading = false;
        }
    }
</script>

<div
    class="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-12 dark:bg-gray-900 sm:px-6 lg:px-8"
>
    <Card.Root class="w-full max-w-md">
        <Card.Header>
            <Card.Title class="text-center text-2xl font-bold"
                >Sign in to your account</Card.Title
            >
            <Card.Description class="text-center">
                Enter your credentials to access the dashboard
            </Card.Description>
        </Card.Header>
        <Card.Content>
            <form onsubmit={handleLogin} class="space-y-4">
                {#if error}
                    <div
                        class="rounded-md bg-red-50 p-4 text-sm text-red-700 dark:bg-red-900/30 dark:text-red-400"
                    >
                        {error}
                    </div>
                {/if}
                <div class="space-y-2">
                    <Label for="username">Username</Label>
                    <Input
                        id="username"
                        type="text"
                        bind:value={username}
                        required
                        placeholder="admin"
                    />
                </div>
                <div class="space-y-2">
                    <Label for="password">Password</Label>
                    <Input
                        id="password"
                        type="password"
                        bind:value={password}
                        required
                        placeholder="••••••••"
                    />
                </div>
                <Button type="submit" class="w-full" disabled={isLoading}>
                    {#if isLoading}
                        <Loader2 class="mr-2 h-4 w-4 animate-spin" />
                        Signing in...
                    {:else}
                        Sign in
                    {/if}
                </Button>
            </form>
        </Card.Content>
        <Card.Footer
            class="flex flex-col space-y-2 text-center text-sm text-gray-500"
        >
            {#if SECURITY_DEV_MODE}
                <p>Demo credentials:</p>
                <div class="flex justify-center space-x-4">
                    <span>admin / admin</span>
                    <span>evaluator / evaluator</span>
                </div>
            {:else}
                <p>Use your issued credentials.</p>
            {/if}
        </Card.Footer>
    </Card.Root>
</div>
