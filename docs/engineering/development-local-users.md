# Development Local Users

Local email/password authentication is available only when `LOCAL_AUTH_ENABLED=true` and is blocked when
`APP_SECURITY_PRODUCTION=true`. Production authentication remains Microsoft Entra ID SSO.

For a disposable local or Replit development workspace, configure these values through `.env.local` or Replit Secrets:

```text
LOCAL_AUTH_ENABLED=true
VITE_LOCAL_AUTH_ENABLED=true
DEV_ADMIN_ENABLED=true
DEV_ADMIN_EMAIL=admin@smartwfm.local
DEV_ADMIN_PASSWORD=Admin@12345
DEV_ADMIN_TENANT_ID=dev-tenant
```

Prefer `DEV_ADMIN_PASSWORD_HASH` with a BCrypt value for any shared environment. On startup, the application creates
this development Administrator and hashed credential only when that email or its Administrator assignment/credential
does not already exist. It never overwrites an existing password hash.

Remove the plaintext secret after generating and storing `DEV_ADMIN_PASSWORD_HASH`. Never commit `.env.local` or place
development credentials in production configuration.
