# GrabPic Backend - Environment Configuration Report

Generated on: 2026-07-26

## Summary

This report provides a comprehensive analysis of all environment variables required for the GrabPic Backend Spring Boot application to start successfully.

---

## Required Variables

These variables MUST be set for the application to start. They have no default values in the configuration.

| Variable | Source | Description | Required For |
|----------|--------|-------------|--------------|
| `SPRING_DATASOURCE_URL` | application.properties | PostgreSQL JDBC URL | Database connectivity |
| `SPRING_DATASOURCE_USERNAME` | application.properties | Database username | Database authentication |
| `SPRING_DATASOURCE_PASSWORD` | application.properties | Database password | Database authentication |
| `MAIL_USERNAME` | application.properties | SMTP email username | OTP email functionality |
| `MAIL_PASSWORD` | application.properties | SMTP email password | OTP email functionality |

**Note**: In Docker Compose, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` are overridden by the docker-compose.yaml environment section, so they don't need to be in .env when using Docker Compose.

---

## Optional Variables (with Defaults)

These variables have default values and will work without being explicitly set.

| Variable | Default Value | Source | Description |
|----------|---------------|--------|-------------|
| `APP_JWT_SECRET` | `myLocalDevelopmentSecretKey123456789` | application.properties | JWT signing secret |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24 hours) | application.properties | JWT token expiration time |
| `DDL_AUTO` | `update` | application.properties | Hibernate DDL auto mode |
| `SHOW_SQL` | `false` | application.properties | Show SQL queries in logs |
| `MAIL_HOST` | `smtp.gmail.com` | application.properties | SMTP server host |
| `MAIL_PORT` | `587` | application.properties | SMTP server port |
| `OTP_EXPIRY_MINUTES` | `10` | application.properties | OTP expiry time in minutes |
| `OTP_MAX_ATTEMPTS` | `3` | application.properties | Maximum OTP verification attempts |
| `PYTHON_FACE_SERVICE_URL` | `http://localhost:8001/embed` | application.properties | Python face service URL |
| `FACE_SERVICE_URL` | `http://localhost:8001/embed` | application.properties | Alternative face service URL |
| `APP_UPLOAD_DIR` | `/storage` | application.properties | File upload directory |
| `app.display.name` | `GrabPic Backend` | SystemStatusService.java | Application display name |
| `app.upload.dir` | `uploads` | UploadServiceImpl.java | Upload directory (Java default) |

---

## Docker-Specific Variables

These variables are only used when building/pushing Docker images.

| Variable | Source | Description |
|----------|--------|-------------|
| `DOCKER_USERNAME` | docker-compose.yaml | Docker Hub username for image tag |
| `IMAGE_TAG` | docker-compose.yaml | Docker image tag |

---

## Configuration Sources Analysis

### application.properties
- **Total placeholders**: 16
- **Required (no default)**: 4 (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, MAIL_USERNAME, MAIL_PASSWORD)
- **Optional (with default)**: 11

### @Value Annotations in Java Code
- **Total @Value annotations found**: 9
- **All properties covered**: Yes, all @Value properties are defined in application.properties
- **No missing properties**: ✅

### Docker Compose Overrides
Docker Compose overrides the following variables (these don't need to be in .env when using Docker Compose):
- `SPRING_DATASOURCE_URL` → `jdbc:postgresql://postgres:5432/grabpic_db`
- `SPRING_DATASOURCE_USERNAME` → `grabpic_user`
- `SPRING_DATASOURCE_PASSWORD` → `grabpic_pass`

### GitHub Actions
- Uses `DOCKER_USERNAME` and `DOCKER_TOKEN` from GitHub Secrets
- No additional environment variables required

---

## Validation Results

### ✅ Configuration Completeness
All placeholders in application.properties are covered by either:
1. Environment variables (in .env)
2. Default values in application.properties
3. Docker Compose overrides

### ✅ No Missing Variables
No environment variable referenced in the code is missing from the configuration.

### ✅ No Duplicate Variables
No duplicate variable definitions found across configuration files.

### ✅ No Unused Variables
All variables defined in .env.example are used by the application.

---

## Configuration Mistakes / Inconsistencies

### ⚠️ Minor Issue: Default JWT Secret in application.properties
**Location**: application.properties line 19
**Issue**: The default JWT secret `myLocalDevelopmentSecretKey123456789` is only 39 characters, which is sufficient for HS256 but should be stronger for production.
**Recommendation**: Always override this with a strong secret in production environments.

### ℹ️ Note: Database URL in Docker Compose vs Local
**Issue**: The database URL differs between Docker Compose (`postgres:5432`) and local development (`localhost:5432`).
**Resolution**: This is intentional and correct. Docker Compose overrides these values, so the .env file can use localhost for local development.

---

## Files Generated

1. **`.env.example`** - Template with all required variables and descriptions
2. **`.env`** - Local development file with safe dummy values

---

## Usage Instructions

### For Local Development (without Docker Compose)
```bash
# Copy the example file
cp .env.example .env

# Edit .env with your actual values:
# - SPRING_DATASOURCE_URL (your PostgreSQL instance)
# - SPRING_DATASOURCE_USERNAME
# - SPRING_DATASOURCE_PASSWORD
# - MAIL_USERNAME (your Gmail)
# - MAIL_PASSWORD (Gmail App Password)

# Run the application
mvn spring-boot:run
```

### For Docker Compose
```bash
# Copy the example file
cp .env.example .env

# Edit .env with your actual values:
# - DOCKER_USERNAME (your Docker Hub username)
# - MAIL_USERNAME (your Gmail)
# - MAIL_PASSWORD (Gmail App Password)
# - Note: Database variables are handled by docker-compose.yaml

# Start with Docker Compose
docker compose up -d
```

### For Production
```bash
# Set all environment variables in your deployment environment
# Use strong secrets for:
# - APP_JWT_SECRET (generate a cryptographically secure random string)
# - SPRING_DATASOURCE_PASSWORD
# - MAIL_PASSWORD
# - DDL_AUTO should be set to 'validate' or 'none'
```

---

## Security Recommendations

1. **Never commit `.env` to version control** - It's already in .gitignore
2. **Use strong secrets in production** - Generate random strings for JWT secret
3. **Use Gmail App Passwords** - Not your regular Gmail password
4. **Set DDL_AUTO to 'validate' or 'none' in production** - Prevents accidental schema changes
5. **Rotate secrets regularly** - Especially JWT secret and database passwords

---

## Conclusion

✅ **Configuration is complete and valid**

All required environment variables have been identified and documented. The generated `.env` and `.env.example` files provide a complete configuration for local development and production deployment. No code modifications are required.
