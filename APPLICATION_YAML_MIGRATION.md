# Application Configuration Migration Guide

## Properties to YAML Migration

### What Changed

The application configuration has been migrated from `application.properties` to `application.yml` format.

## File Comparison

### Before (application.properties)
```properties
spring.application.name=OrderService
spring.datasource.url=jdbc:mysql://localhost:3306/project2_m2
spring.datasource.username=root
spring.datasource.password=root
resilience4j.circuitbreaker.instances.productServiceCircuitBreaker.failure-rate-threshold=50
```

### After (application.yml)
```yaml
spring:
  application:
    name: OrderService
  datasource:
    url: jdbc:mysql://localhost:3306/project2_m2
    username: root
    password: root
resilience4j:
  circuitbreaker:
    instances:
      productServiceCircuitBreaker:
        failure-rate-threshold: 50
```

## Why YAML?

✅ **Better Readability** - Hierarchical structure is clearer
✅ **Less Repetition** - No need to repeat property prefixes
✅ **Easier Editing** - Indentation makes structure obvious
✅ **Less Error-Prone** - Syntax is simpler
✅ **More Maintainable** - Grouped related properties together

## Configuration Structure

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/project2_m2
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### Server Configuration
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
```

### Resilience4j Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      productServiceCircuitBreaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  retry:
    instances:
      productServiceRetry:
        max-attempts: 3
        wait-duration: 500ms
```

## Using application.yml

### Spring Boot automatically detects application.yml
- No changes needed in code
- Spring Boot loads application.yml by default
- All configurations work exactly the same way

### Multiple profiles (optional)
You can create environment-specific files:
```
application.yml           # Default configuration
application-dev.yml       # Development configuration
application-prod.yml      # Production configuration
application-staging.yml   # Staging configuration
```

### Activate a specific profile
```bash
# In command line
java -jar app.jar --spring.profiles.active=prod

# In application.yml
spring:
  profiles:
    active: prod
```

## Customization Examples

### Change Database Connection
```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/prod_db
    username: prod_user
    password: prod_password
```

### Make Circuit Breaker More Aggressive
```yaml
resilience4j:
  circuitbreaker:
    instances:
      productServiceCircuitBreaker:
        failure-rate-threshold: 30  # Open at 30% instead of 50%
        wait-duration-in-open-state: 15s  # Recover faster
```

### Increase Retry Attempts
```yaml
resilience4j:
  retry:
    instances:
      productServiceRetry:
        max-attempts: 5  # Retry more times
        wait-duration: 1s  # Wait longer between retries
```

### Enable External Payment Service
```yaml
payment:
  service:
    enabled: true  # Use external payment service
```

## Migration Checklist

- ✅ Created `application.yml` with all configurations
- ✅ All properties converted to YAML format
- ✅ Database configuration migrated
- ✅ Server configuration migrated
- ✅ Logging configuration migrated
- ✅ Resilience4j configuration migrated
- ✅ Management endpoints configuration migrated

## Next Steps

### Option 1: Use YAML Only (Recommended)
```bash
# Delete the old properties file
rm src/main/resources/application.properties

# Use only application.yml
```

### Option 2: Keep Both (During Migration)
- Keep both files during transition
- Spring Boot will use application.yml preferentially
- Remove application.properties once you're confident

## Common YAML Syntax

### Simple Values
```yaml
server:
  port: 8080
  servlet:
    context-path: /api
```

### Lists
```yaml
resilience4j:
  retry:
    instances:
      productServiceRetry:
        retry-exceptions:
          - java.util.concurrent.TimeoutException
          - java.net.ConnectException
```

### Nested Objects
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: root
```

### Strings with Special Characters
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## Troubleshooting

### Configuration Not Loading
- Check YAML syntax (indentation is important)
- Ensure file is in `src/main/resources/`
- Check Spring Boot logs for errors

### Profile Not Active
- Verify profile name is correct
- Check `application-{profile}.yml` file exists
- Use `--spring.profiles.active=profile` when running

### Port Already in Use
- Change `server.port` in YAML
- Or kill the process using that port
- Try: `netstat -ano | findstr :8080` (Windows)

## Validation

### Check Configuration is Loaded
```bash
# Check health endpoint
curl http://localhost:8080/api/actuator/health

# Check circuit breaker config
curl http://localhost:8080/api/actuator/circuitbreakers
```

### Verify Values
All configurations from `application.yml` should be active and match what you set.

## Benefits of This Change

✅ More readable and maintainable code
✅ Easier to manage multiple environments
✅ Better IDE support with auto-completion
✅ Cleaner structure for complex configurations
✅ Following modern Spring Boot conventions

## Additional Resources

- YAML Syntax: [yaml.org](https://yaml.org)
- Spring Boot Properties: [docs.spring.io](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- YAML in Spring: [Spring Boot YAML Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.yaml)

---

**Status**: ✅ Migration Complete
**File**: `application.yml`
**Location**: `src/main/resources/application.yml`
**All configurations**: Fully migrated and functional

