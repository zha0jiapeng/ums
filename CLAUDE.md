# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UMS (User Management System) is a Spring Boot 2.7.8 application providing user authentication, management, and WeChat integration.

- **Framework:** Spring Boot 2.7.8 with Java 8
- **Build Tool:** Maven
- **Database:** MySQL 8.0.33 with MyBatis-Plus 3.5.1
- **Key Features:** JWT authentication, WeChat Mini Program/Official Account integration, Redis caching, internationalization support

## Common Commands

### Build & Run
```bash
# Compile the project
mvn clean compile

# Build package
mvn clean package

# Run application (defaults to prod profile)
mvn spring-boot:run

# Run with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Skip tests during build
mvn clean package -DskipTests
```

### Development
```bash
# Clean build artifacts
mvn clean

# Check dependencies
mvn dependency:tree

# Format code (if formatter plugin configured)
mvn formatter:format
```

## Architecture & Code Organization

### Layered Architecture
The codebase follows a clean 3-tier architecture:

```
com.global.ums/
├── controller/      REST API endpoints
├── service/         Business logic interfaces
├── service/impl/    Business logic implementations
├── mapper/          MyBatis-Plus data access layer
├── entity/          Database entities
├── dto/             Data transfer objects
├── config/          Spring configuration classes
├── interceptor/     HTTP interceptors (JWT auth, locale)
├── utils/           Utility classes
├── annotation/      Custom annotations (@RequireAuth, @BrotliCompress)
└── constant/        Constants and enums
```

### Key Design Patterns

**Service Layer:**
- All services extend `IService<Entity>` and implementations extend `ServiceImpl<Mapper, Entity>`
- This provides automatic CRUD operations via MyBatis-Plus
- Custom business logic added as additional methods

**Response Pattern:**
- All API responses wrapped in `AjaxResult` (extends HashMap)
- Standard format: `{"code": 0, "msg": "success", "data": {...}}`
- Use `AjaxResult.success()`, `AjaxResult.error()`, or `AjaxResult.successI18n(key)` for i18n

**Query Pattern:**
```java
LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
queryWrapper.eq(User::getType, type).like(User::getUniqueId, uniqueId);
Page<User> page = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
```

## Authentication & Authorization

### JWT Implementation
- **Access Token:** 24 hours expiration
- **Refresh Token:** 30 days expiration
- **Header:** `Authorization: GlbUmsBearer <token>`
- **Utilities:** `JwtUtils.java` for token generation/validation
- **Context:** `LoginUserContextHolder` provides ThreadLocal user context access

### Protected Endpoints
- Controllers/methods annotated with `@RequireAuth` require JWT authentication
- Interceptor: `JwtAuthInterceptor` validates tokens and populates user context
- Excluded paths: `/user/login`, `/user/register`, `/wechat/**`, Swagger endpoints

### User Context Access
```java
Long userId = LoginUserContextHolder.getUserId();
String username = LoginUserContextHolder.getUsername();
Integer userType = LoginUserContextHolder.getUserType();
```

**Important:** Context is automatically cleared after request completion. Don't access it outside request scope.

## Database & Persistence

### MyBatis-Plus Conventions
- **Entities:** `@TableName("table_name")` annotation
- **Mappers:** Extend `BaseMapper<Entity>` with `@Mapper` annotation
- **Automatic CRUD:** Available via `IService` interface (save, remove, update, getById, list, page)
- **Pagination:** Use `Page<Entity>` object, automatically populated with total count

### User Properties System
The system uses a flexible key-value property system:
- **Entity:** `UserProperties` stores key-value pairs with configurable data types
- **Validation:** `KeyValidationUtils` validates property keys against `UmsPropertyKeys` table
- **Scope:** Properties can be user-scoped or system-scoped
- **Storage:** Values stored as `byte[]` for flexibility

### Configuration Keys
Property key definitions moved from `key-validation-config.json` to database table `ums_property_keys`:
- Reload config: `KeyValidationUtils.reloadConfig()`
- Validate before use: `ValidationResult result = KeyValidationUtils.validateKey(key, fileSize)`

## Configuration Management

### Application Profiles
- **application.yml** - Production defaults (default profile)
- **application-local.yml** - Local development overrides
- **application-prod.yml** - Production-specific settings

### Property Keys Auto-Refresh
The system automatically refreshes property key configurations from the database:

**Auto-Refresh Mechanism:**
- **Scheduled Task**: Runs every 30 seconds (configurable via `property.keys.refresh.interval`)
- **Cache Miss Fallback**: Immediately queries database when a key is not in cache
- **Manual Refresh**: Still available via `POST /system/property-keys/refresh`

**Configuration:**
```yaml
property:
  keys:
    refresh:
      interval: 30000  # Milliseconds, default 30s
```

**Behavior:**
- Add new keys directly to database → Available within 30s (or immediately on first use)
- No application restart needed
- No manual API call required
- Three-tier caching strategy for optimal performance

### Key Configuration Areas

**JWT Settings:**
```yaml
jwt:
  secret: UMSSecretKey
  expiration: 86400              # Access token (24 hours)
  refresh-expiration: 2592000    # Refresh token (30 days)
  token-header: Authorization
  token-prefix: GlbUmsBearer
```

**WeChat Integration:**
```yaml
wx:
  qrcodeExpireSecond: 300
  mp:                             # Official Account
    appId: wx-appid
    appSecret: wx-secret
    token: wx-token
    encodingAESKey: wx-aes-key
  ma:                             # Mini Program
    appId: wx-appid
    appSecret: wx-secret
```

**Database (Druid):**
```yaml
spring.datasource.druid:
  url: jdbc:mysql://localhost:3306/ums_db
  username: root
  password: password
  initialSize: 5
  minIdle: 10
  maxActive: 20
```

**Redis:**
```yaml
spring.redis:
  host: localhost
  port: 6379
  database: 0
```

## Internationalization (I18N)

### Configuration
- Message files: `messages.properties` (Chinese), `messages_en.properties` (English)
- Switch language: Add `?lang=zh_CN` or `?lang=en_US` query parameter
- Default locale: Simplified Chinese

### Usage in Code
```java
// Controller responses
return AjaxResult.successI18n("user.add.success");
return AjaxResult.errorI18n("auth.captcha.error");

// Service layer
String message = MessageUtils.message("user.not.found");
```

### Adding New Messages
1. Add key-value to `messages.properties` (Chinese)
2. Add corresponding translation to `messages_en.properties`
3. Use `MessageUtils.message(key)` or `AjaxResult.*I18n(key)` in code

## Security Considerations

### Password Handling
- **Encryption:** MD5 with salt, 1000 iterations (see `PasswordUtils.java`)
- **Salt:** 16 bytes, Base64 encoded, generated per user
- **Verification:** Always use `PasswordUtils.verifyPassword(input, salt, encrypted)`

### CAPTCHA
- **Generation:** `CaptchaController.captchaImage()` supports Math/Char types
- **Storage:** Stored in Redis with expiration
- **Validation:** Required before login attempts

## External Service Integration

### WeChat Services
- **Mini Program:** Configured via `WxMaConfig.java`, service bean `wxMaService`
- **Official Account:** Configured via `WxMpConfig.java`, service bean `wxMpService`
- **QR Code Login:** Full flow in `MiniappController` (generate, scan, confirm/cancel)
- **Phone Number:** Decrypt WeChat encrypted phone data via Mini Program API

### Redis Usage
- **Utility:** `RedisCache` class provides typed operations
- **Common uses:** CAPTCHA cache, session state, WeChat config cache, QR code state
- **Expiration:** Always set TTL for temporary data (CAPTCHA codes, QR codes)

```java
// String operations
redisCache.setCacheObject(key, value, timeout, TimeUnit.MINUTES);

// Hash operations
redisCache.setCacheMapValue(mapKey, hashKey, value);
Object value = redisCache.getCacheMapValue(mapKey, hashKey);
```

## API Documentation

- **Knife4j UI:** http://localhost:port/doc.html
- **Swagger UI:** http://localhost:port/swagger-ui.html
- **OpenAPI JSON:** http://localhost:port/v2/api-docs

Controllers are automatically discovered. Use standard Spring annotations for documentation.

## Custom Annotations

### @RequireAuth
```java
@RequireAuth                    // Apply to controller class or method
public class UserController {
    // All methods require authentication
}
```

### @BrotliCompress
```java
@BrotliCompress(
    enabled = true,
    quality = 6,                // 0-11, higher = better compression
    threshold = 1024,           // Min response size in bytes
    window = 22                 // Window size bits (10-24)
)
public AjaxResult largeResponse() {
    // Response will be Brotli compressed if client supports it
}
```

## Key Utility Classes

### JwtUtils
- `generateToken(userId, userType, username)` - Create access token
- `generateRefreshToken(userId)` - Create refresh token
- `parseToken(token)` - Extract claims
- `validateToken(token)` - Check signature and expiration

### RedisCache
- Typed operations for String, List, Set, Map, Hash
- Automatic expiration management
- Thread-safe operations

### KeyValidationUtils
- `reloadConfig()` - Reload property keys from database
- `validateKey(key, fileSize)` - Validate property key against rules
- Returns `ValidationResult` with validation details

### PasswordUtils
- `generateSalt()` - Generate random salt
- `encryptPassword(password, salt)` - Hash password with salt
- `verifyPassword(input, salt, encrypted)` - Verify password match

## Common Development Workflows

### Adding New Entity/CRUD
1. Create entity class in `entity/` with `@TableName` annotation
2. Create mapper interface in `mapper/` extending `BaseMapper<Entity>`
3. Create service interface in `service/` extending `IService<Entity>`
4. Create service impl in `service/impl/` extending `ServiceImpl<Mapper, Entity>`
5. Create controller in `controller/` with REST endpoints
6. Add i18n messages for operations

### Adding New API Endpoint
1. Add method to controller with appropriate mapping (`@GetMapping`, `@PostMapping`, etc.)
2. Add `@RequireAuth` if authentication required
3. Implement business logic in service layer
4. Return `AjaxResult` wrapper
5. Add i18n messages for success/error cases

### Adding New User Property Key
1. Insert into `ums_property_keys` table with validation rules
2. Call `KeyValidationUtils.reloadConfig()` or restart application
3. Use validated key in `UserProperties` operations

## Important Implementation Notes

### ThreadLocal Cleanup
- `LoginUserContextHolder` uses ThreadLocal for user context
- Context automatically cleared in interceptor's `afterCompletion()`
- Don't manually store in ThreadLocal without proper cleanup

### Transaction Management
- Use `@Transactional(rollbackFor = Exception.class)` on service methods that modify data
- Service layer is the appropriate place for transaction boundaries

### Response Compression
- Brotli compression available via `@BrotliCompress` annotation
- Requires client to send `Accept-Encoding: br` header
- Automatic fallback if client doesn't support Brotli

### Pagination
- MyBatis-Plus pagination automatically enabled via `MybatisPlusConfig`
- Use `Page<Entity>` objects for paginated queries
- Total count automatically populated

## File Locations Reference

### Configuration
- `src/main/resources/application*.yml` - Application configuration
- `src/main/resources/messages*.properties` - I18N messages
- `src/main/resources/db/migration/` - Database initialization scripts

### Core Components
- `UmsServerApplication.java` - Application entry point
- `config/WebMvcConfig.java` - MVC configuration, CORS, interceptors
- `config/MybatisPlusConfig.java` - Database pagination configuration
- `interceptor/JwtAuthInterceptor.java` - Authentication logic

### Key Business Logic
- `controller/auth/AuthController.java` - Login/logout endpoints
- `controller/user/UserController.java` - User management
- `controller/miniapp/MiniappController.java` - WeChat Mini Program integration
- `service/impl/*ServiceImpl.java` - Business logic implementations
