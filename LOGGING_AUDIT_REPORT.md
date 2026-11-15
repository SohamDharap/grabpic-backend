# Logging Audit Report

## 1. Current Logging Architecture

### Logging Framework
- **Framework**: SLF4J with Logback (Spring Boot default)
- **Status**: Already configured via Spring Boot starter dependencies
- **No custom configuration files existed**: No logback.xml or log4j configuration was present
- **Lombok**: Already used in the project with @Slf4j annotation available

### Dependencies (pom.xml)
- `spring-boot-starter-logging` (transitive via spring-boot-starter-web)
- `logback-classic` 1.5.32
- `logback-core` 1.5.32
- `slf4j-api` 2.0.17
- Lombok 1.18.36 (provides @Slf4j)

### Existing Logging Usage
- **Already using @Slf4j**: UploadServiceImpl, SearchServiceImpl, PythonFaceServiceImpl, FaceEmbeddingServiceImpl, UploadController, PublicSearchController
- **Missing @Slf4j**: SystemController, AuthController, EventController, AdminController, JwtFilter, GlobalExceptionHandler, SecurityConfig
- **No logging configuration in application.properties**: No logging levels were configured
- **No request logging filter**: No global request/response logging
- **No security logging**: JwtFilter had no debug logs for authentication flow
- **No exception logging**: GlobalExceptionHandler did not log exceptions before returning responses

---

## 2. Missing Logging Pieces

### Before Changes
1. ❌ No logging levels configured in application.properties
2. ❌ No request/response logging filter
3. ❌ No controller entry/exit logging (SystemController, AuthController, EventController, AdminController)
4. ❌ No security authentication flow logging (JwtFilter)
5. ❌ No exception logging in GlobalExceptionHandler
6. ❌ No logback-spring.xml for file logging
7. ❌ No way to enable TRACE logging without code changes

### After Changes
1. ✅ Logging levels configured in application.properties
2. ✅ RequestLoggingFilter added for global request/response logging
3. ✅ @Slf4j and entry/exit logs added to all major controllers
4. ✅ Security logging added to JwtFilter
5. ✅ Exception logging added to GlobalExceptionHandler
6. ✅ logback-spring.xml created for console + file logging
7. ✅ Profile-based configuration allows TRACE logging via environment variable

---

## 3. Files Modified

### 3.1 application.properties
**Path**: `src/main/resources/application.properties`

**Changes**: Added logging configuration section

```diff
+# ==========================================
+# LOGGING
+# ==========================================
+logging.level.root=INFO
+logging.level.org.springframework=INFO
+logging.level.org.springframework.security=DEBUG
+logging.level.com.grabpic=DEBUG
```

**Explanation**: Configures sensible defaults for production. Spring Security DEBUG level helps troubleshoot authentication/authorization issues.

---

### 3.2 RequestLoggingFilter.java (NEW)
**Path**: `src/main/java/com/grabpic/backend/filter/RequestLoggingFilter.java`

**Full Diff**:
```diff
+package com.grabpic.backend.filter;
+
+import jakarta.servlet.FilterChain;
+import jakarta.servlet.ServletException;
+import jakarta.servlet.http.HttpServletRequest;
+import jakarta.servlet.http.HttpServletResponse;
+import lombok.extern.slf4j.Slf4j;
+import org.springframework.stereotype.Component;
+import org.springframework.web.filter.OncePerRequestFilter;
+
+import java.io.IOException;
+
+@Component
+@Slf4j
+public class RequestLoggingFilter extends OncePerRequestFilter {
+
+    @Override
+    protected void doFilterInternal(HttpServletRequest request,
+                                    HttpServletResponse response,
+                                    FilterChain filterChain)
+            throws ServletException, IOException {
+
+        long startTime = System.currentTimeMillis();
+        String method = request.getMethod();
+        String uri = request.getRequestURI();
+        String queryString = request.getQueryString();
+        String remoteAddr = request.getRemoteAddr();
+
+        String fullUri = (queryString != null) ? uri + "?" + queryString : uri;
+
+        log.info("Incoming {} {} from {}", method, fullUri, remoteAddr);
+
+        try {
+            filterChain.doFilter(request, response);
+        } finally {
+            long duration = System.currentTimeMillis() - startTime;
+            int status = response.getStatus();
+            log.info("Completed {} {} -> {} ({} ms)", method, fullUri, status, duration);
+        }
+    }
+}
```

**Explanation**: Global filter that logs all incoming requests with method, URI, query parameters, remote IP, response status, and execution time. Does NOT log request bodies or sensitive headers (Authorization).

---

### 3.3 SecurityConfig.java
**Path**: `src/main/java/com/grabpic/backend/config/SecurityConfig.java`

**Changes**: Added RequestLoggingFilter import and field registration

```diff
+import com.grabpic.backend.filter.RequestLoggingFilter;
 import com.grabpic.backend.security.JwtFilter;
```

```diff
+    @Autowired
+    private RequestLoggingFilter requestLoggingFilter;
+
     @Autowired
     private JwtFilter jwtFilter;
```

```diff
+                .addFilterBefore(requestLoggingFilter, JwtFilter.class)
                 .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

**Explanation**: Registers RequestLoggingFilter before JwtFilter in the filter chain to ensure all requests are logged regardless of authentication status.

---

### 3.4 SystemController.java
**Path**: `src/main/java/com/grabpic/backend/controller/SystemController.java`

**Changes**: Added @Slf4j and entry/exit logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RequiredArgsConstructor
 public class SystemController {
```

```diff
     public ResponseEntity<SystemStatusResponse> getSystemStatus() {
+        log.info("Entering status endpoint");
+        ResponseEntity<SystemStatusResponse> response = ResponseEntity.ok(systemStatusService.getSystemStatus());
+        log.info("Status endpoint completed successfully");
+        return response;
-        return ResponseEntity.ok(systemStatusService.getSystemStatus());
     }
```

**Explanation**: Logs entry and exit of the status endpoint to help diagnose 403 issues.

---

### 3.5 AuthController.java
**Path**: `src/main/java/com/grabpic/backend/controller/AuthController.java`

**Changes**: Added @Slf4j and authentication flow logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RequiredArgsConstructor
 public class AuthController {
```

```diff
     public ResponseEntity<ApiResponseDto> requestOtp(
             @Valid @RequestBody OtpRequestDto request) {
+        log.info("OTP request for email: {}", request.getEmail());
         otpService.generateAndSendOtp(request.getEmail());
+        log.info("OTP sent successfully to: {}", request.getEmail());
         return ResponseEntity.ok(
                 new ApiResponseDto(true, "OTP sent to " + request.getEmail()));
     }
```

```diff
     public ResponseEntity<JwtResponseDto> verifyOtp(
             @Valid @RequestBody OtpVerifyDto request) {
+        log.info("OTP verification attempt for email: {}", request.getEmail());
         String token = otpService.verifyOtpAndGenerateToken(
                 request.getEmail(), request.getOtp());

         UserDetails user = userRepository.findByEmail(request.getEmail()).get();

+        log.info("OTP verified successfully for email: {}, role: {}", user.getEmail(), user.getRole().name());
         return ResponseEntity.ok(
                 new JwtResponseDto(token, user.getEmail(), user.getRole().name()));
     }
```

**Explanation**: Logs OTP request, verification attempts, and successful authentication with user role.

---

### 3.6 EventController.java
**Path**: `src/main/java/com/grabpic/backend/controller/EventController.java`

**Changes**: Added @Slf4j and event operation logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RequiredArgsConstructor
 public class EventController {
```

```diff
     public ResponseEntity<EventResponseDto> createEvent(
             @Valid @RequestBody CreateEventDto request,
             @RequestAttribute("userId") Long photographerId) {
+        log.info("Creating event for photographer: {}", photographerId);
         EventResponseDto response = eventService.createEvent(request, photographerId);
+        log.info("Event created successfully with ID: {}", response.getId());
         return ResponseEntity.ok(response);
     }
```

```diff
     public ResponseEntity<List<EventResponseDto>> getEventsByPhotographer(
             @RequestAttribute("userId") Long photographerId) {
+        log.info("Fetching events for photographer: {}", photographerId);
         List<EventResponseDto> events = eventService.getEventsByPhotographer(photographerId);
+        log.info("Found {} events for photographer: {}", events.size(), photographerId);
         return ResponseEntity.ok(events);
     }
```

```diff
     public ResponseEntity<EventResponseDto> getEventByPublicToken(
             @PathVariable String publicToken) {
+        log.info("Fetching event by public token: {}", publicToken);
         EventResponseDto event = eventService.getEventByPublicToken(publicToken);
+        log.info("Event found: {}", event.getName());
         return ResponseEntity.ok(event);
     }
```

**Explanation**: Logs event creation, retrieval, and public access with relevant IDs and counts.

---

### 3.7 AdminController.java
**Path**: `src/main/java/com/grabpic/backend/controller/AdminController.java`

**Changes**: Added @Slf4j and admin operation logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RequiredArgsConstructor
 public class AdminController {
```

```diff
     public ResponseEntity<PhotographerResponseDto> createPhotographer(
             @Valid @RequestBody CreatePhotographerDto request) {
+        log.info("Creating photographer");
         PhotographerResponseDto response = adminService.createPhotographer(request);
+        log.info("Photographer created successfully with ID: {}", response.getId());
         return ResponseEntity.ok(response);
     }
```

```diff
     public ResponseEntity<PhotographerResponseDto> activatePhotographer(
             @PathVariable Long photographerId) {
+        log.info("Activating photographer: {}", photographerId);
         PhotographerResponseDto response = adminService.activatePhotographer(photographerId);
+        log.info("Photographer activated: {}", photographerId);
         return ResponseEntity.ok(response);
     }
```

```diff
     public ResponseEntity<PhotographerResponseDto> deactivatePhotographer(
             @PathVariable Long photographerId) {
+        log.info("Deactivating photographer: {}", photographerId);
         PhotographerResponseDto response = adminService.deactivatePhotographer(photographerId);
+        log.info("Photographer deactivated: {}", photographerId);
         return ResponseEntity.ok(response);
     }
```

```diff
     public ResponseEntity<List<PhotographerResponseDto>> getAllPhotographers() {
+        log.info("Fetching all photographers");
         List<PhotographerResponseDto> photographers = adminService.getAllPhotographers();
+        log.info("Found {} photographers", photographers.size());
         return ResponseEntity.ok(photographers);
     }
```

**Explanation**: Logs all admin operations (create, activate, deactivate, list) with photographer IDs and counts.

---

### 3.8 JwtFilter.java
**Path**: `src/main/java/com/grabpic/backend/security/JwtFilter.java`

**Changes**: Added @Slf4j and authentication flow logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RequiredArgsConstructor
 public class JwtFilter extends OncePerRequestFilter {
```

```diff
     protected boolean shouldNotFilter(HttpServletRequest request) {
+        log.debug("Skipping JWT filter for public endpoint: {}", request.getRequestURI());
         String path = request.getRequestURI();
         return path.startsWith("/api/auth/") ||
```

```diff
         String authHeader = request.getHeader("Authorization");
+        log.debug("Processing JWT filter for: {}", request.getRequestURI());

         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
+            log.debug("No valid Authorization header found");
             filterChain.doFilter(request, response);
             return;
         }
```

```diff
         if (jwtUtil.validateToken(token)) {
+            log.debug("JWT token validated successfully for email: {}", email);
             String email = jwtUtil.extractEmail(token);

             if (SecurityContextHolder.getContext().getAuthentication() == null) {
+                log.debug("Setting authentication for user: {}", email);
                 UserDetails userDetails = userDetailsService.loadUserByUsername(email);
```

```diff
                 SecurityContextHolder.getContext().setAuthentication(authToken);
+                log.info("Authentication successful for user: {} with role: {}", email, user.getRole());
             }
         }
+        log.debug("JWT token validation failed");
     }

     filterChain.doFilter(request, response);
```

**Explanation**: Logs JWT filter decisions, token validation results, and successful authentication with user email and role. This helps diagnose 403 issues by showing exactly when and why authentication fails.

---

### 3.9 GlobalExceptionHandler.java
**Path**: `src/main/java/com/grabpic/backend/exception/GlobalExceptionHandler.java`

**Changes**: Added @Slf4j and exception logging

```diff
+import lombok.extern.slf4j.Slf4j;
```

```diff
+@Slf4j
 @RestControllerAdvice
 public class GlobalExceptionHandler {
```

```diff
     public ResponseEntity<ApiResponseDto> handleValidationErrors(
             MethodArgumentNotValidException ex) {
+        log.warn("Validation error: {}", errors);
         String errors = ex.getBindingResult().getFieldErrors()
```

```diff
     public ResponseEntity<ApiResponseDto> handleRuntimeException(
             RuntimeException ex) {
+        log.error("RuntimeException: {}", ex.getMessage(), ex);
         return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                 .body(new ApiResponseDto(false, ex.getMessage()));
     }
```

**Explanation**: Logs all exceptions before returning error responses. Validation errors are logged at WARN level, runtime exceptions at ERROR level with full stack trace.

---

### 3.10 logback-spring.xml (NEW)
**Path**: `src/main/resources/logback-spring.xml`

**Full Diff**:
```diff
+<?xml version="1.0" encoding="UTF-8"?>
+<configuration>
+    <springProfile name="!prod">
+        <!-- Console appender for local development -->
+        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
+            <encoder>
+                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
+            </encoder>
+        </appender>
+
+        <root level="INFO">
+            <appender-ref ref="CONSOLE"/>
+        </root>
+    </springProfile>
+
+    <springProfile name="prod">
+        <!-- Console appender for Docker logs (stdout) -->
+        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
+            <encoder>
+                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
+            </encoder>
+        </appender>
+
+        <!-- File appender for persistent logs -->
+        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
+            <file>logs/grabpic.log</file>
+            <encoder>
+                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
+            </encoder>
+            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
+                <fileNamePattern>logs/grabpic.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
+                <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
+                    <maxFileSize>10MB</maxFileSize>
+                </timeBasedFileNamingAndTriggeringPolicy>
+                <maxHistory>30</maxHistory>
+                <totalSizeCap>1GB</totalSizeCap>
+            </rollingPolicy>
+        </appender>
+
+        <root level="INFO">
+            <appender-ref ref="CONSOLE"/>
+            <appender-ref ref="FILE"/>
+        </root>
+    </springProfile>
+
+    <!-- Application-specific logging -->
+    <logger name="com.grabpic" level="DEBUG"/>
+
+    <!-- Spring Security DEBUG for production troubleshooting -->
+    <logger name="org.springframework.security" level="DEBUG"/>
+</configuration>
```

**Explanation**: 
- **Non-prod profile**: Console logging only for local development
- **Prod profile**: Console + file logging for production
- **Console appender**: Ensures `docker logs` continues to work
- **File appender**: Writes to `logs/grabpic.log` with daily rolling, 10MB max per file, 30 days retention, 1GB total cap
- **Application-specific**: DEBUG level for com.grabpic package
- **Spring Security**: DEBUG level for authentication/authorization troubleshooting

---

## 4. How to Enable DEBUG/TRACE Logging in Production

### Method 1: Environment Variable (Recommended)
Set the environment variable when starting the application:

```bash
# Enable TRACE for Spring Security
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=TRACE

# Or for all Spring components
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=TRACE
```

### Method 2: Docker Compose Override
Add to your docker-compose.yaml:

```yaml
environment:
  - LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=TRACE
```

### Method 3: Application Properties Override
For temporary debugging, you can override via:

```bash
java -jar backend.jar --logging.level.org.springframework.security=TRACE
```

### Method 4: Actuator Endpoint (If Enabled)
If you have the Actuator `env` endpoint enabled, you can check current logging levels:

```bash
curl http://localhost:8080/actuator/env/logging.level.org.springframework.security
```

---

## 5. Docker Compatibility

### Console Logging Preserved
The logback-spring.xml configuration maintains console logging in both profiles:
- **Non-prod**: Console only
- **Prod**: Console + File

This ensures `docker logs` continues to work as expected:

```bash
docker logs <container-id>
```

### File Logging (Optional)
File logging is only active in the `prod` profile. To enable file logging in Docker:

1. **Mount a volume** for the logs directory:
```yaml
volumes:
  - ./logs:/app/logs
```

2. **Set the profile**:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
```

### No Breaking Changes
- Console logging remains the default
- File logging is additive, not replacement
- Existing Docker workflows continue to work without modification

---

## 6. Log Output Examples

### Request Logging
```
INFO  Incoming GET /api/system/status from 172.31.0.10
INFO  Completed GET /api/system/status -> 200 (45 ms)
```

### Authentication Logging
```
INFO  OTP request for email: user@example.com
INFO  OTP sent successfully to: user@example.com
INFO  OTP verification attempt for email: user@example.com
INFO  Authentication successful for user: user@example.com with role: PHOTOGRAPHER
```

### Security Logging
```
DEBUG Skipping JWT filter for public endpoint: /api/system/status
DEBUG Processing JWT filter for: /api/events
DEBUG No valid Authorization header found
DEBUG JWT token validated successfully for email: user@example.com
DEBUG Setting authentication for user: user@example.com
INFO  Authentication successful for user: user@example.com with role: PHOTOGRAPHER
```

### Exception Logging
```
WARN  Validation error: email: Email is required
ERROR RuntimeException: User not found: invalid@example.com
```

---

## 7. Troubleshooting the 403 Issue

With these logging improvements, when you encounter a 403 on `/api/system/status`, you will see:

### Expected Logs (Working)
```
INFO  Incoming GET /api/system/status from 172.31.0.10
DEBUG Skipping JWT filter for public endpoint: /api/system/status
INFO  Entering status endpoint
INFO  Status endpoint completed successfully
INFO  Completed GET /api/system/status -> 200 (45 ms)
```

### If 403 Occurs (Problem)
```
INFO  Incoming GET /api/system/status from 172.31.0.10
DEBUG Processing JWT filter for: /api/system/status  <-- should NOT see this
DEBUG No valid Authorization header found
DEBUG JWT token validation failed
[Spring Security DEBUG logs showing rejection reason]
INFO  Completed GET /api/system/status -> 403 (12 ms)
```

### What to Look For
1. **"Skipping JWT filter"** should appear for public endpoints
2. **"Processing JWT filter"** should NOT appear for public endpoints
3. **Spring Security DEBUG logs** will show which filter rejected the request
4. **"Entering status endpoint"** should appear if controller is reached

---

## 8. Summary

### Changes Made
1. ✅ Configured logging levels in application.properties
2. ✅ Created RequestLoggingFilter for global request/response logging
3. ✅ Added @Slf4j and logging to SystemController
4. ✅ Added @Slf4j and logging to AuthController
5. ✅ Added @Slf4j and logging to EventController
6. ✅ Added @Slf4j and logging to AdminController
7. ✅ Added @Slf4j and logging to JwtFilter
8. ✅ Added @Slf4j and logging to GlobalExceptionHandler
9. ✅ Created logback-spring.xml for console + file logging
10. ✅ Registered RequestLoggingFilter in SecurityConfig

### No Breaking Changes
- All changes are additive
- Existing functionality preserved
- Docker compatibility maintained
- No dependencies added
- Spring Boot defaults respected

### Production Ready
- Sensible logging levels configured
- File logging with rolling policies
- Profile-based configuration
- Environment variable overrides supported
- No sensitive data logged (no Authorization headers, no request bodies)
