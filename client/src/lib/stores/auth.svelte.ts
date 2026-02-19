import { browser } from '$app/environment';
import { CLIENT_AUTO_AUTH, SECURITY_DEV_MODE } from '$lib/config.js';

class AuthStore {
    token = $state<string | null>(null);
    user = $state<any | null>(null);

    constructor() {
        if (browser) {
            if (CLIENT_AUTO_AUTH && SECURITY_DEV_MODE) {
                // Auto-authenticate with a dev user
                this.token = 'dev-token';
                this.user = { username: 'admin', role: 'ADMIN' };
            } else {
                this.token = localStorage.getItem('token');
                const savedUser = localStorage.getItem('user');
                if (savedUser) {
                    this.user = JSON.parse(savedUser);
                }
            }
        }
    }

    setToken(token: string) {
        this.token = token;
        if (browser) {
            localStorage.setItem('token', token);
        }
    }

    setUser(user: any) {
        this.user = user;
        if (browser) {
            localStorage.setItem('user', JSON.stringify(user));
        }
    }

    logout() {
        this.token = null;
        this.user = null;
        if (browser) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    }

    get isAuthenticated() {
        return !!this.token;
    }
}

export const auth = new AuthStore();
