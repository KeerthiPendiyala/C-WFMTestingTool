# Dependency Update Policy

Dependencies are pinned in `backend/pom.xml`, `frontend/package.json`, `.github/workflows/ci.yml`, and infrastructure files.

Update policy:

- Do not float dependency versions.
- Update one dependency family at a time unless a security advisory requires a coordinated upgrade.
- Run frontend lint, typecheck, unit tests, Playwright smoke tests, backend tests, migration tests, and the full package build after updates.
- Do not add Azure, broker, AI-provider, connector, or monitoring SDKs to business modules. Provider SDKs belong behind adapter implementations only.
- Keep Replit startup free of Docker Compose, RabbitMQ, Azure Key Vault, Azure Blob Storage, and Application Insights requirements.
- Record behavior-changing dependency upgrades in architecture or operations docs.

