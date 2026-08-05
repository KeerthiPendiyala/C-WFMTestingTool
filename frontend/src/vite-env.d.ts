/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_NAME?: string;
  readonly VITE_APP_ENV?: string;
  readonly VITE_ENTRA_CLIENT_ID?: string;
  readonly VITE_ENTRA_AUTHORITY?: string;
  readonly VITE_ENTRA_API_SCOPE?: string;
  readonly VITE_AUTH_REDIRECT_URI?: string;
  readonly VITE_POST_LOGOUT_REDIRECT_URI?: string;
  readonly VITE_LOCAL_AUTH_ENABLED?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
