## Certify Backend

Spring Boot backend for certificate management.

### Prerequisites

- Java 21
- PostgreSQL

### Running the Application

1. Configure database connection in `application.yml` or environment variables
2. Run `./dev.sh` from the `backend` directory
3. API available at `http://localhost:8080`

**Note:** On startup, the application automatically seeds a System Admin user and an initial Tenant Admin user (if bootstrap is enabled via configuration).

### Testing

- Run unit tests: `mvn test`
- Compile: `mvn -q compile`
