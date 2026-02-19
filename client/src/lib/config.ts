export const DEFAULT_TENANT_ID =
    (import.meta.env.VITE_DEFAULT_TENANT_ID as string | undefined)?.trim() ||
    "tenant-001";

export const CLIENT_AUTO_AUTH =
    (import.meta.env.VITE_CLIENT_AUTO_AUTH as string | undefined) === "true";

export const SECURITY_DEV_MODE =
    (import.meta.env.VITE_SECURITY_DEV_MODE as string | undefined) === "true";
