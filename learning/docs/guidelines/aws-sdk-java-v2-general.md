# AWS SDK for Java v2 通用指南

## 通用原则

- 编写清晰、可读、可维护的代码
- 遵循面向对象设计的 SOLID 原则
- 优先使用组合而非继承
- 面向接口编程，而非面向实现
- 快速失败 — 尽早发现并报错
- 修改现有 API 时保持向后兼容

## 代码风格标准

- 遵循 Java 编码约定与代码库既有风格
- 使用能清楚表达用途的有意义的变量、方法与类名
- 为公开 API 提供完整的 Javadoc
- 保持方法短小，单一职责
- 限制方法参数数量（理想情况下不超过 3 个）
- 尽量缩小 API 表面，优先 internal 而非 public
- 保持缩进与格式一致

## 常见设计模式

- 使用构建器模式创建对象
- 异步操作遵循响应式编程原则
- 使用 SdkException 体系进行错误处理
- 在合适场景优先使用不可变对象

## SDK 工具类

AWS SDK for Java v2 提供若干工具类，应优先使用它们，而非外部库或自研实现：

### JSON 解析
- **生产代码**：**必须**使用 `json-utils` 模块中的 `JsonNodeParser`（`core/json-utils/src/main/java/software/amazon/awssdk/protocols/jsoncore/JsonNodeParser.java`）解析 JSON
- **测试代码例外**：测试代码**可以**为方便使用 Jackson、javax.json 等外部 JSON 库
- **代码生成例外**：代码生成模板在合适时**可以**使用外部 JSON 库
- **不得**在生产 SDK 代码中使用外部 JSON 库
- 示例：
  ```java
  JsonNodeParser parser = JsonNodeParser.create();
  JsonNode rootNode = parser.parse(jsonString);
  ```

### 延迟初始化
- **必须**使用 `utils` 模块中的 `Lazy`（`utils/src/main/java/software/amazon/awssdk/utils/Lazy.java`）实现线程安全的延迟初始化
- **不得**自研延迟初始化模式
- 示例：
  ```java
  private static final Lazy<ExpensiveObject> EXPENSIVE_OBJECT = 
      new Lazy<>(() -> new ExpensiveObject());
  
  public ExpensiveObject getExpensiveObject() {
      return EXPENSIVE_OBJECT.getValue();
  }
  ```

### 带 TTL 的缓存
- **必须**使用 `utils` 模块中的 `CachedSupplier`（`utils/src/main/java/software/amazon/awssdk/utils/cache/CachedSupplier.java`）实现带生存时间（TTL）的缓存
- **不得**自研带过期逻辑的缓存机制
- 当需要缓存昂贵操作且需定期刷新时使用 `CachedSupplier`
- 示例：
  ```java
  private final Supplier<AuthToken> tokenCache = CachedSupplier.builder(() -> 
      RefreshResult.builder(fetchAuthToken())
                   .staleTime(Instant.now().plus(Duration.ofMinutes(5)))
                   .build())
      .cachedValueName("AuthToken")
      .build();
  
  public AuthToken getAuthToken() {
      return tokenCache.get();
  }
  
  private AuthToken fetchAuthToken() {
      // Expensive operation to fetch token
      return callAuthService();
  }
  ```
- **主要能力**：
  - 线程安全缓存与自动过期
  - 可配置陈旧时间以失效缓存
  - 可选预取以主动刷新
  - 通过 `cachedValueName` 提供内置日志与调试支持

## 命名约定

详见 [命名约定指南](NamingConventions.md)。

## 类初始化

详见 [优先使用静态工厂方法指南](FavorStaticFactoryMethods.md)。

## Optional 的使用

详见 [Optional 使用指南](UseOfOptional.md)。

## 对象方法（toString、equals、hashCode）

- 所有公开 POJO 类**必须**实现 `toString()`、`equals()` 与 `hashCode()`
- 向现有 POJO 新增字段时，**必须**同步更新上述方法
- 实现要点：
  - **必须**使用 SDK 的 `ToString` 工具类（`utils/src/main/java/software/amazon/awssdk/utils/ToString.java`）保持格式一致：
    ```java
    @Override
    public String toString() {
        return ToString.builder("YourClassName")
                       .add("fieldName1", fieldValue1)
                       .add("fieldName2", fieldValue2)
                       .build();
    }
    ```
  - **不得**包含可能敏感的字段（如凭证）
  - `equals()`：比较所有字段相等性，并妥善处理 null
  - `hashCode()`：将所有字段纳入哈希计算
- **必须**使用 EqualsVerifier 编写单元测试，确保 equals 与 hashCode 覆盖所有字段。示例：
  ```java
  @Test
  public void equalsHashCodeTest() {
      EqualsVerifier.forClass(YourClass.class)
                    .withNonnullFields("requiredFields")
                    .verify();
  }
  ```
- 实现示例见 `core/regions/src/test/java/software/amazon/awssdk/regions/PartitionEndpointKeyTest.java`

## 异常处理

- 避免在公开 API 中抛出受检异常
- 除非能妥善处理，否则不要捕获异常
- 始终提供有意义的错误信息
- 在 `finally` 中清理资源，或使用 try-with-resources
- 不要用异常做流程控制，异常代价高
  ```java
  // 不佳：依赖校验器抛异常
  public ValidationResult validateRequest(Request request) {
      try {
          validateRequired(request);     // Throws if required fields missing
          validateFormat(request);       // Throws if format invalid
          validatePermissions(request);  // Throws if permissions insufficient
          return ValidationResult.success();
      } catch (ValidationException e) {
          return ValidationResult.failure(e.getMessage());
      }
  }

  // 更好：显式校验与返回值
  public ValidationResult validateRequest(Request request) {
      ValidationResult result = validateRequired(request);
      if (!result.isValid()) {
          return result;
      }
    
      result = validateFormat(request);
      if (!result.isValid()) {
          return result;
      }
    
      result = validatePermissions(request);
      return result;
  }
  ```
- **不得**捕获后以相同异常类型重新抛出

```java
// 不佳：捕获后以相同类型重抛
public void processMessage(String message) {
    try {
        parseMessage(message);
    } catch (SnsMessageParsingException e) {
        throw new SnsMessageParsingException(e.getMessage(), e); 
    }
  }
// 不佳：即便附加上下文，也不要捕获并以相同异常类型重抛
public void processMessage(String message) {
    try {
        parseMessage(message);
    } catch (SnsMessageParsingException e) {
        // WRONG - Don't catch and rethrow SnsMessageParsingException as SnsMessageParsingException
        throw SnsMessageParsingException.builder()
            .message("Failed to process SNS message in batch operation. " + e.getMessage() + 
                    " Message index: " + getCurrentMessageIndex())
            .cause(e)
            .build();
    }
} 
```

### 错误信息指南
- **必须**提供与上下文相关的错误信息，说明出错原因及如何修复
- **必须**包含相关细节（字段名、期望格式、实际值等），但不暴露敏感信息
- **必须**正确链接异常链，在补充信息的同时保留原始上下文
- **在合适时**应包含排障指引

