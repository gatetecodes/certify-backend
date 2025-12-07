## Certify Backend

Spring Boot backend for certificate management.

### Prerequisites

- Java 21
- PostgreSQL

### Running the Application

1. **Set up environment variables:**
   - Copy `env.example` to `.env`: `cp env.example .env`
   - Edit `.env` with your configuration values
   - Required environment variables:
     - `DB_URL` - PostgreSQL connection URL
     - `DB_USERNAME` - Database username
     - `DB_PASSWORD` - Database password
     - `JWT_SECRET` - Secret key for JWT token signing (use a secure random string in production)
     - `CERTIFY_BOOTSTRAP_ADMIN_ENABLED` - Set to `true` to enable initial user seeding
     - Bootstrap admin variables (see `env.example` for details)

2. **Run the application:**
   ```bash
   ./dev.sh
   ```
   The `dev.sh` script automatically sources the `.env` file and starts the application.

3. API available at `http://localhost:8080`

**Note:** On startup, the application automatically seeds a System Admin user and an initial Tenant Admin user (if bootstrap is enabled via configuration). See `env.example` for all available environment variables.

### Testing

- Run unit tests: `mvn test`
- Compile: `mvn -q compile`
