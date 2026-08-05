import {
  BrowserCacheLocation,
  LogLevel,
  type Configuration,
  type RedirectRequest
} from '@azure/msal-browser';

const env = import.meta.env;

export const appEnvironment = env.VITE_APP_ENV ?? 'development';
const configuredAppName = env.VITE_APP_NAME?.trim();
export const appName =
  configuredAppName && configuredAppName.length > 0
    ? configuredAppName
    : 'Smart WFM Test Management';
export const localAuthEnabled = env.VITE_LOCAL_AUTH_ENABLED === 'true';

function currentOrigin() {
  return typeof window === 'undefined' ? 'http://localhost:5173' : window.location.origin;
}

export const redirectUri = env.VITE_AUTH_REDIRECT_URI ?? `${currentOrigin()}/auth/callback`;
export const postLogoutRedirectUri =
  env.VITE_POST_LOGOUT_REDIRECT_URI ?? `${currentOrigin()}/auth/logout`;

export const msalConfig: Configuration = {
  auth: {
    clientId: env.VITE_ENTRA_CLIENT_ID ?? '',
    authority: env.VITE_ENTRA_AUTHORITY ?? '',
    redirectUri,
    postLogoutRedirectUri
  },
  cache: {
    cacheLocation: BrowserCacheLocation.SessionStorage
  },
  system: {
    loggerOptions: {
      logLevel: LogLevel.Warning,
      piiLoggingEnabled: false
    }
  }
};

export const apiScope = env.VITE_ENTRA_API_SCOPE ?? '';
export const isSsoConfigured = Boolean(
  msalConfig.auth.clientId && msalConfig.auth.authority && apiScope
);

export const loginRequest: RedirectRequest = {
  scopes: apiScope ? [apiScope] : []
};
