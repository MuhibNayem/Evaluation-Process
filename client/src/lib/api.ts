import axios from 'axios';
import { auth } from './stores/auth.svelte.js';
import { goto } from '$app/navigation';
import { browser } from '$app/environment';

const api = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json'
    }
});

api.interceptors.request.use((config) => {
    if (browser && auth.token) {
        config.headers.Authorization = `Bearer ${auth.token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

api.interceptors.response.use((response) => {
    return response;
}, (error) => {
    if (browser && error.response && error.response.status === 401) {
        auth.logout();
        goto('/login');
    }
    return Promise.reject(error);
});

export default api;
