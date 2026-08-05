# OpenAI Requirement Generation

The requirement-generation API extracts uploaded PDF, DOCX, DOC, or CSV content inside the Spring Boot backend, sends only the extracted text to OpenAI, validates the strict structured response, and transactionally stores the document metadata, generation job, generated requirements, and audit records.

The browser never receives the OpenAI API key and never calls OpenAI directly.

## Environment variables

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `OPENAI_API_KEY` | Yes for AI generation | Empty | Server-only OpenAI API credential. Configure it as a Replit Secret or deployment secret. |
| `OPENAI_MODEL` | No | `gpt-5.6-sol` | Responses API model used for structured requirement generation. |
| `OPENAI_BASE_URL` | No | `https://api.openai.com/v1` | OpenAI API base URL. |
| `OPENAI_CONNECT_TIMEOUT` | No | `10s` | Connection timeout understood by Spring duration parsing. |
| `OPENAI_READ_TIMEOUT` | No | `3m` | Maximum time to wait for a generation response. |
| `AI_MAX_EXTRACTED_CHARACTERS` | No | `500000` | Maximum extracted character count accepted from one document. |

`OPENAI_API_KEY` must not be placed in Vite variables, frontend `.env` files, source code, logs, or API responses. The application can start without the key; generation returns a safe `503` Problem Details response until the server secret is configured.

## Replit

Add `OPENAI_API_KEY` and optionally `OPENAI_MODEL` in Replit Secrets. The existing single-port build and run commands remain unchanged. Apache Tika runs in-process and supports all four required document formats without Docker, a broker, or an external conversion service.
