## Certify Backend – Certificate Management Service

This Spring Boot application provides the backend for the **Sec CERTIFICATE** assignment.
It manages tenants, users, certificate templates, certificate generation (sync and async), PDF rendering, and public verification.

### Architecture overview

- **Tech stack**
  - **Java 17**, **Spring Boot 3**, **Spring Security**, **Spring Data JPA**
  - **PostgreSQL** (JSONB + Flyway migrations)
  - **JWT** for stateless authentication
  - **openhtmltopdf** for HTML → PDF rendering
  - **ZXing** for QR-code generation

- **Core modules**
  - `auth`: login endpoint returning a JWT plus user identity and role.
  - `tenant`: system-admin APIs for onboarding tenants and tenant admins.
  - `template`: tenant-scoped certificate templates with placeholders and HTML bodies.
  - `certificate`: generation, storage, revocation, and async jobs.
  - `pdf` / `qr`: infrastructure for rendering PDFs and QR codes.
  - `security`: JWT filter, security config, and multi-tenant context setup.
  - `common`: base entities and `TenantContextHolder` for tenant scoping.

### How multi-tenancy and security work

- After login, the frontend stores the JWT from `/api/auth/login`.
- Every authenticated request includes `Authorization: Bearer <token>`.
- `JwtAuthenticationFilter`:
  - Validates the token and loads the user.
  - Extracts the `tenantId` claim and sets it in `TenantContextHolder`.
  - Populates Spring Security’s `SecurityContext`.
- Service methods that are tenant-aware call `requireTenant()`, which reads `TenantContextHolder`:
  - Repository calls are always tenant-scoped (e.g. `findAllByTenantId`, `findByIdAndTenantId`).
  - Without a tenant in context, services throw `IllegalStateException("No tenant in context")`.
- Method-level security:
  - `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` for tenant admin APIs (`TenantAdminController`).
  - `@PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")` for template and certificate APIs.

### Mapping to assignment requirements

- **Onboard customer to the system**
  - Endpoint: `TenantAdminController` under `/api/admin/tenants` (system-admin only).
  - Creates a tenant plus an initial tenant admin user.
- **Add certificate template**
  - Endpoint: `TemplateController` under `/api/v1/templates` (tenant admin).
  - Stores HTML template, placeholder definitions, and versioning.
- **Simulate certificate generation**
  - Endpoint: `POST /api/v1/certificates/simulate`.
  - Applies placeholders and renders a PDF preview with a temporary verification QR code.
- **Generate certificate via API**
  - Sync: `POST /api/v1/certificates` (returns `CertificateResponse` + verification URL).
  - Async (high throughput): `POST /api/v1/certificates/async` + `GET /api/v1/certificates/jobs/{id}`.
- **Download certificate**
  - Endpoint: `GET /api/v1/certificates/{id}/download`.
- **Verify certificate**
  - Public endpoint: `GET /public/verify/{publicId}` via `VerificationController`.
  - Computes hash of stored PDF and compares it with the token checksum.
  - Returns `{ valid, reason, certificateId, templateId, issuedAt }`.

### Fraud prevention and revocation

- Each generated certificate:
  - Is rendered to PDF and stored via `FileSystemStorageService`.
  - Has a SHA‑256 hash stored in the DB.
  - Gets a `CertificateVerificationToken` with a public verification id and checksum.
  - The PDF contains a QR code pointing at the verification URL.
- Revocation:
  - Endpoint: `POST /api/v1/certificates/{id}/revoke` (tenant admin only).
  - Sets status to `REVOKED` and optionally stores a `revocationReason` in the data JSON.
  - Verification endpoint returns `reason="REVOKED"` and `valid=false` for revoked certificates.

### Async pipeline for 1000+ certificates/min

- **Job model**
  - Table `certificate_jobs` with fields such as `tenant_id`, `template_id`, `request_data_json`, `status`, `requested_by`, `certificate_id`, and `error_message`.
  - JPA entity: `CertificateJob` with enum `CertificateJobStatus { PENDING, PROCESSING, COMPLETED, FAILED }`.
- **Submission & status APIs**
  - `POST /api/v1/certificates/async`:
    - Accepts the same payload as the sync generate endpoint.
    - Persists a `PENDING` job and returns its id and status.
  - `GET /api/v1/certificates/jobs/{id}`:
    - Tenant-scoped lookup returning job status and the generated `certificateId` once completed.
- **Background workers**
  - `CertificateJobProcessor` is a scheduled component (`@EnableScheduling` on `CertifyApplication`).
  - Every poll (configurable with `certify.jobs.poll-interval`):
    - Fetches up to 50 `PENDING` jobs.
    - For each job, sets `TenantContextHolder` and calls `CertificateService.processJob`.
    - Updates job status to `COMPLETED` or `FAILED` with an error message.
- **Scaling story**
  - Multiple application instances can run in parallel; each executes `CertificateJobProcessor`.
  - Throughput scales with:
    - Number of instances.
    - Batch size per poll.
    - Worker poll interval.
  - PostgreSQL acts as a durable queue; jobs are idempotent via status transitions.

### Running the backend locally

1. **Prerequisites**
   - Java 17
   - PostgreSQL running and accessible.
2. **Configure environment**
   - Create a `.env` file next to `dev.sh` with database and JWT settings, for example:
     - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/certify`
     - `SPRING_DATASOURCE_USERNAME=certify`
     - `SPRING_DATASOURCE_PASSWORD=certify`
3. **Start the app**
   - From the `backend` directory run:
     - `./dev.sh`
   - The API is available on `http://localhost:8080`.
4. **Build & tests**
   - `mvn -q compile` to compile.
   - `mvn test` to run unit tests (requires a JDK with working attach mechanism for Mockito).
