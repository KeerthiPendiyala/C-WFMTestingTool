# Microsoft Entra SSO Setup

This document supports AUTH-01 through AUTH-03 and UI-01. It records configuration names only; do not store client secrets, tokens, certificates or connection strings in this repository.

## App Registrations

Create separate Microsoft Entra app registrations, or equivalent exposed API/application pairs, for each environment.

SPA registration:
- Platform type: Single-page application.
- Redirect URIs: register each deployed origin plus `/auth/callback`, for example local development, enterprise host names, Replit `*.replit.app` domains and custom domains.
- Logout redirect URI: register the matching `/auth/logout` URI where the tenant requires it.
- Authentication flow: authorization-code flow with PKCE through MSAL React.
- Secrets: none in the SPA.

API registration:
- Expose an API scope for the Spring resource server audience.
- Configure access-token issuance for the SPA client.
- The backend validates token signature, issuer, audience, expiry, not-before, tenant ID and object ID.

## Environment Variables

Frontend:
- `VITE_ENTRA_CLIENT_ID`
- `VITE_ENTRA_AUTHORITY`
- `VITE_ENTRA_API_SCOPE`
- `VITE_AUTH_REDIRECT_URI`
- `VITE_POST_LOGOUT_REDIRECT_URI`
- `VITE_APP_ENV`
- `VITE_LOCAL_AUTH_ENABLED`

Backend:
- `OAUTH2_RESOURCE_SERVER_ENABLED`
- `ENTRA_ALLOWED_TENANTS`
- `ENTRA_AUDIENCES`
- `ENTRA_ISSUER_HOST`
- `ENTRA_JWK_SET_URI`
- `LOCAL_AUTH_ENABLED`
- `LOCAL_ADMIN_USERNAME`
- `LOCAL_ADMIN_PASSWORD`
- `LOCAL_ADMIN_PASSWORD_HASH`
- `LOCAL_ADMIN_TENANT_ID`
- `LOCAL_ADMIN_OBJECT_ID`
- `APP_SECURITY_PRODUCTION`
- `BOOTSTRAP_ADMIN_ENABLED`
- `BOOTSTRAP_ADMIN_EMAIL`

## Replit Callback Setup

For each Replit deployment, register the public Replit origin as a SPA redirect URI with `/auth/callback`. If a custom domain is mapped, register that custom-domain callback URI as well. The application resolves redirect URIs from environment/config, so no code change is required when domains change.

## User Linking

Users must be pre-provisioned with first name, last name and normalized contact email. First successful SSO login binds the approved user to the token `tid` and immutable `oid`. Email, preferred username and name are stored only as display/contact claim data and are never authorization keys after binding.

## Administrator Bootstrap

The first Administrator can be created by migration or by a one-time startup process:

1. Pre-provision the user record.
2. Set `BOOTSTRAP_ADMIN_ENABLED=true` and `BOOTSTRAP_ADMIN_EMAIL` to the normalized pre-provisioned email.
3. Start the app once.
4. Remove or set `BOOTSTRAP_ADMIN_ENABLED=false` before the next startup.

If an active Administrator assignment already exists while bootstrap remains enabled, startup fails. This keeps the bootstrap path one-time and environment-controlled.

## Local Auth

The email/password form from the source screenshot is development/test only. Set both frontend and backend local-auth flags explicitly to show the labelled non-production form. Backend startup fails if local auth is enabled while the production flag/profile is active.

Local auth is Administrator-only. The username must match an existing active pre-provisioned user that has a global Administrator assignment. The password is supplied only through environment variables, preferably as `LOCAL_ADMIN_PASSWORD_HASH`; `LOCAL_ADMIN_PASSWORD` is supported for disposable local development. The local path creates a server-side session and never issues a custom SPA token, stores a password in the repository, overwrites an Entra binding, or activates invited users.
