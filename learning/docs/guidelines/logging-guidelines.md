# AWS SDK for Java v2 日志指南

## 目录
- [通用原则](#通用原则)
- [日志级别](#日志级别)
- [结构化日志](#结构化日志)
- [敏感数据处理](#敏感数据处理)
- [错误日志](#错误日志)
- [测试日志](#测试日志)

## 通用原则

### 核心日志标准
- **使用 SDK Logger**：始终使用 `software.amazon.awssdk.utils.Logger`，不要直接使用 SLF4J 或其他日志框架
- **信息有意义**：编写清晰、可操作的日志，便于调试
- **包含上下文**：补充请求 ID、操作名等相关上下文
- **保持一致**：全库使用一致的格式与术语
- **考虑读者**：为实际排障的人写日志（往往不是你本人）
  - 提供足够上下文，使不熟悉代码的人也能理解发生了什么
  - 避免只有原作者能懂的内部行话或缩写
  - 包含业务上下文，而非仅有技术细节
  - 设想凌晨 3 点生产故障时需要哪些信息

### Logger 声明
- **静态 final**：Logger 声明为 `private static final`
- **按类创建**：使用类本身创建 Logger

```java
// Good: Proper logger declaration
private static final Logger logger = Logger.loggerFor(MyClass.class);
```

## 日志级别

### 级别指引
在 SDK 中一致地使用日志级别：

**重要**：不要滥用 WARN 与 ERROR。仅当问题**不会**通过异常暴露给用户时才使用 WARN/ERROR。若会向调用方抛出异常，不要再记 WARN 或 ERROR — 否则会造成重复报错与日志噪音。异常本身就是面向用户的错误通道。当 WARN/ERROR 条件影响**每一次**请求（例如已弃用配置、缺失可选功能、次优设置）时，使用「只打一次」模式，避免每条请求刷屏。每个 JVM 进程生命周期内只记录一次，而非每次请求。

**ERROR**：系统错误、阻止操作完成且**未**向调用方抛出的异常
- 不导致对外抛异常的内部系统或回调失败
- 后台进程失败
- 资源清理失败

```java
// ERROR: Critical failures that don't throw exceptions to caller
logger.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be ignored.", e);
logger.error(() -> "Connection pool health check failed [pool=" + poolName + "]", exception);

// BAD: Don't log ERROR if you're throwing the exception to the caller
public void validateInput(String input) {
    if (input == null) {
        logger.error(() -> "Input validation failed: null input"); // DON'T DO THIS
        throw new IllegalArgumentException("Input cannot be null"); // Exception already tells the user
    }
}
```

**WARN**：可恢复问题、已弃用用法、**未**向调用方抛异常时的性能顾虑
- 成功的降级操作（用户看不到失败）
- 已弃用 API 的使用（操作仍成功）
- 性能下降（操作完成但较慢）
- 带成功兜底的配置问题

```java
logger.warn(() -> "Primary endpoint failed, falling back to secondary [primary=" + 
                  primaryEndpoint + ", secondary=" + secondaryEndpoint + "]");
logger.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be used for testing.");

// BAD: Don't log WARN if the failure will be thrown to caller
public Response callService() {
    try {
        return primaryService.call();
    } catch (ServiceException e) {
        logger.warn(() -> "Service call failed", e); // DON'T DO THIS
        throw e; // Exception already informs the caller
    }
}

// GOOD: Log WARN only when you handle the error internally
public Response callServiceWithFallback() {
    try {
        return primaryService.call();
    } catch (ServiceException e) {
        logger.warn(() -> "Primary service failed, using fallback [error=" + e.getMessage() + "]");
        return fallbackService.call(); // User gets successful response
    }
}
```

**INFO**：不要在 AWS SDK for Java v2 中使用 INFO 级别。

对可能有用的运行信息请改用 DEBUG：

**DEBUG**：运行信息、详细执行流、参数值
- 服务启停
- 主要操作完成
- 配置变更
- 重要状态迁移
- 带参数的方法进入/退出
- 中间处理步骤
- 配置细节
- 性能指标

```java
// DEBUG: Operational and detailed execution information
logger.debug(() -> "Retry attempt [attemptNumber=" + attemptNumber + 
                   ", delay=" + delayMs + "ms]");
```

**TRACE**：非常详细的执行信息，通常用于排障
- 详细状态
- 细粒度耗时

```java
 logger.debug(() -> "Interceptor '" + interceptor + "' modified the message with its " + methodName + " method.");
 // TRACE: detailed execution information
logger.trace(() -> "Old: " + originalMessage + "\nNew: " + newMessage);
```            
## 结构化日志

### 一致格式
在适用时使用一致的键值格式表达结构化数据：

```java
// Good: Consistent structured format
logger.debug(() -> "Operation completed [operation=" + operationName + 
                   ", duration=" + duration + "ms, status=" + status + 
                   ", itemCount=" + itemCount + "]");
```

## 敏感数据处理

切勿记录敏感信息。

**禁止记录：**
- HTTP 头字段值
- 身份凭证
- 带有 sensitive trait 的成员

**可以记录：**
- 请求 ID、关联 ID
- 操作名、状态码
- 耗时与性能指标

### 错误上下文
提供足够上下文以便调试：

```java
// Good: Rich error context
s3AsyncClient.abortMultipartUpload(request)
             .exceptionally(throwable -> {
                 logger.warn(() -> String.format("Failed to abort previous multipart upload. You may need to call S3AsyncClient#abortMultiPartUpload to "
                 + "free all storage consumed by all parts. [id=%s]", uploadId), throwable);
                 return null;
            });
```

## 测试日志

### 使用 LogCaptor
AWS SDK 提供 `software.amazon.awssdk.testutils.LogCaptor` 用于测试日志输出：

- **创建 LogCaptor**：使用 `LogCaptor.create(ClassName.class)` 捕获指定类的日志
- **设置级别**：使用 `logCaptor.setLevel(Level.DEBUG)` 等捕获特定级别
- **访问事件**：使用 `logCaptor.loggedEvents()` 获取已捕获的日志事件
- **清理**：始终在 `@AfterEach` 中调用 `logCaptor.close()`，避免内存泄漏

### 测试中的日志级别
在单元测试中验证重要日志消息，并用 `LogCaptor` 校验级别是否合适：

```java
@Test
void processRequest_withFallback_logsAtWarnLevel() {
    Request request = createRequestThatTriggersFailover();
    
    Response response = service.processRequestWithFallback(request);
    
    assertThat(response).isNotNull(); // Operation succeeded via fallback
    
    List<LogEvent> warnEvents = logCaptor.loggedEvents().stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .collect(Collectors.toList());
    
    assertThat(warnEvents).hasSize(1);
    assertThat(warnEvents.get(0).getMessage())
        .contains("Primary service failed, using fallback");
}

@Test
void processRequest_withValidation_logsAtDebugLevel() {
    logCaptor.setLevel(Level.DEBUG);
    Request request = createInvalidRequest();
    
    assertThatThrownBy(() -> service.processRequest(request))
        .isInstanceOf(ValidationException.class);
    
    List<LogEvent> debugEvents = logCaptor.loggedEvents().stream()
        .filter(event -> event.getLevel() == Level.DEBUG)
        .collect(Collectors.toList());
    
    assertThat(debugEvents).isNotEmpty();
    assertThat(debugEvents.get(0).getMessage()).contains("Input validation failed");
}
```
