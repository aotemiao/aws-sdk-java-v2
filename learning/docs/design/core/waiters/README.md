**设计：** 新功能，**状态：** [已发布](../../README.md)

# Waiters（等待器）

「Waiters」是一种抽象，用于轮询资源直至达到期望状态，或判定资源永远不会进入该状态。AWS Java SDK 1.x 已支持该能力；本文说明在 Java SDK 2.x 中的实现方式。

## 简介

Waiter 让客户更易等待资源进入期望状态，在服务端操作为异步时尤其有用。

例如，调用 `dynamodb#createTable` 时，服务会立即返回 `TableStatus` 为 `CREATING` 的响应，在状态变为 `ACTIVE` 之前表不可读写。可使用 Waiter 等待表变为可用。

## 拟议 API

SDK 2.x 将为具备 waiter 资格操作的服务客户端同时提供同步与异步 waiter，并提供泛型 `Waiter` 类，使客户可自定义轮询函数、定义成功/失败/重试条件及 `maxAttempts` 等配置。

### 使用示例

#### 示例 1：使用同步 waiter

- 从已有服务客户端实例化 waiter 对象

```Java
DynamoDbClient client = DynamoDbClient.create();
DynamodbWaiter waiter = client.waiter();

WaiterResponse<DescribeTableResponse> response = waiter.waitUntilTableExists(b -> b.tableName("table"));
```

- 从 waiter 构建器实例化 waiter 对象

```java
DynamodbWaiter waiter = DynamoDbWaiter.builder()
                                      .client(client)
                                      .overrideConfiguration(p -> p.maxAttempts(10))
                                      .build();

WaiterResponse<DescribeTableResponse> response = waiter.waitUntilTableExists(b -> b.tableName("table"));

```

#### 示例 2：使用异步 waiter

- 从已有服务客户端实例化 waiter 对象

```Java
DynamoDbAsyncClient asyncClient = DynamoDbAsyncClient.create();
DynamoDbAsyncWaiter waiter = asyncClient.waiter();

CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = waiter.waitUntilTableExists(b -> b.tableName("table"));

```

- 从 waiter 构建器实例化 waiter 对象

```java
DynamoDbAsyncWaiter waiter = DynamoDbAsyncWaiter.builder()
                                                .client(asyncClient)
                                                .overrideConfiguration(p -> p.maxAttempts(10))
                                                .build();

CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = waiter.waitUntilTableExists(b -> b.tableName("table"));
```


*下文 FAQ：「为何不在客户端上直接提供 waiter 操作？」*

#### 示例 3：使用泛型 waiter

```Java
Waiter<DescribeTableResponse> waiter =
   Waiter.builder(DescribeTableResponse.class)
         .addAcceptor(WaiterAcceptor.successAcceptor(r -> r.table().tableStatus().equals(TableStatus.ACTIVE)))
         .addAcceptor(WaiterAcceptor.retryAcceptor(t -> t instanceof ResourceNotFoundException))
         .addAcceptor(WaiterAcceptor.errorAcceptor(t -> t instanceof InternalServerErrorException))
         .overrideConfiguration(p -> p.maxAttemps(20).backoffStrategy(BackoffStrategy.defaultStrategy())
         .build();

// 同步运行
WaiterResponse<DescribeTableResponse> response = waiter.run(() -> client.describeTable(describeTableRequest));

// 异步运行
CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture =
      waiter.runAsync(() -> asyncClient.describeTable(describeTableRequest));
```

### `{Service}Waiter` 与 `{Service}AsyncWaiter`

每个具备 waiter 资格的服务将生成两个类：`{Service}Waiter` 与 `{Service}AsyncWaiter`（例如 `DynamoDbWaiter`、`DynamoDbAsyncWaiter`）。
命名策略与现有的 `{Service}Client`、`{Service}Utilities` 一致。

#### 示例

```Java
/**
 * Waiter utility class that waits for a resource to transition to the desired state.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface DynamoDbWaiter extends SdkAutoCloseable {

    /**
     * Poller method that waits for the table status to transition to <code>ACTIVE</code> by
     * invoking {@link DynamoDbClient#describeTable}. It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param describeTableRequest Represents the input of a <code>DescribeTable</code> operation.
     * @return {@link DescribeTableResponse}
     */
    default WaiterResponse<DescribeTableResponse> waitUntilTableExists(DescribeTableRequest describeTableRequest) {
        throw new UnsupportedOperationException();
    }

    default WaiterResponse<DescribeTableResponse> waitUntilTableExists(Consumer<DescribeTableRequest.Builder> describeTableRequest) {
        return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build());
    }

     /**
      * Polls {@link DynamoDbAsyncClient#describeTable} API until the desired condition {@code TableExists} is met, or
      * until it is determined that the resource will never enter into the desired state
      *
      * @param describeTableRequest
      *        The request to be used for polling
      * @param overrideConfig
      *        Per request override configuration for waiters
      * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
      *         condition
      */
     default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(
             DescribeTableRequest describeTableRequest, WaiterOverrideConfiguration overrideConfig) {
         throw new UnsupportedOperationException();
     }
 
     /**
      * Polls {@link DynamoDbAsyncClient#describeTable} API until the desired condition {@code TableExists} is met, or
      * until it is determined that the resource will never enter into the desired state.
      * <p>
      * This is a convenience method to create an instance of the request builder and instance of the override config
      * builder
      *
      * @param describeTableRequest
      *        The consumer that will configure the request to be used for polling
      * @param overrideConfig
      *        The consumer that will configure the per request override configuration for waiters
      * @return WaiterResponse containing either a response or an exception that has matched with the waiter success
      *         condition
      */
     default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(
             Consumer<DescribeTableRequest.Builder> describeTableRequest,
             Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
         return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build(),
                 WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
     }

    // other waiter operations omitted
    // ...


    interface Builder {
    
        Builder client(DynamoDbClient client);
        
        /**
         * Defines overrides to the default SDK waiter configuration that should be used for waiters created from this
         * builder
         *
         * @param overrideConfiguration
         *        the override configuration to set
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);

        DynamoDbWaiter build();
    }
}

/**
 * Waiter utility class that waits for a resource to transition to the desired state asynchronously.
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public interface DynamoDbAsyncWaiter extends SdkAutoCloseable {

    /**
     * Poller method that waits for the table status to transition to <code>ACTIVE</code> by
     * invoking {@link DynamoDbClient#describeTable}. It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param describeTableRequest Represents the input of a <code>DescribeTable</code> operation.
     * @return A CompletableFuture containing the result of the DescribeTable operation returned by the service. It completes
     * successfully when the resource enters into a desired state or it completes exceptionally when it is determined that the
     * resource will never enter into the desired state.
     */
    default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(DescribeTableRequest describeTableRequest) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(Consumer<DescribeTableRequest.Builder> describeTableRequest) {
        return waitUntilTableExists(DescribeTableRequest.builder().applyMutation(describeTableRequest).build());
    }

    // other waiter operations omitted
    // ...


    interface Builder {
        
        Builder client(DynamoDbAsyncClient client);

        Builder scheduledExecutorService(ScheduledExecutorService executorService);

        Builder overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);

        DynamoDbAsyncWaiter build();
    }

}
```

*下文 FAQ：「为何返回 WaiterResponse 包装类」*。

#### 实例化

可从已有服务客户端或构建器创建该类实例。

- 从已有服务客户端

```Java
// sync waiter
DynamoDbClient dynamo = DynamoDbClient.create();
DynamoDbWaiter dynamoWaiter = dynamo.waiter();

// async waiter
DynamoDbClient dynamoAsync = DynamoDbAsyncClient.create();
DynamoDbAsyncWaiter dynamoAsyncWaiter = dynamoAsync.waiter();
```

- 从 waiter 构建器

```java
// sync waiter
DynamodbWaiter waiter = DynamoDbWaiter.builder()
              .client(client)
              .overrideConfiguration(p -> p.maxAttempts(10))
              .build();


// async waiter
DynamoDbAsyncWaiter asyncWaiter = DynamoDbAsyncWaiter.builder()
                           .client(asyncClient)
                           .overrideConfiguration(p -> p.maxAttempts(10))
                           .build();


```

#### 方法

每个需要 waiter 支持的操作都会生成对应方法。按期望成功状态分为两类。

  - 同步：`WaiterResponse<{Operation}Response> waitUntil{DesiredState}({Operation}Request)`
    ```java
    WaiterResponse<DescribeTableResponse> waitUntilTableExists(DescribeTableRequest describeTableRequest)
    ```
  - 异步：`CompletableFuture<WaiterResponse<{Operation}Response>> waitUntil{DesiredState}({Operation}Request)`
    ```java
    CompletableFuture<WaiterResponse<DescribeTableResponse>> waitUntilTableExists(DescribeTableRequest describeTableRequest)
    ```

### `WaiterResponse<T>`
```java
/**
 * The response returned from a waiter operation
 * @param <T> the type of the response
 */
@SdkPublicApi
public interface WaiterResponse<T> {

    /**
     * @return the ResponseOrException union received that has matched with the waiter success condition
     */
    ResponseOrException<T> matched();

    /**
     * @return the number of attempts executed
     */
    int attemptsExecuted();

}
```

*下文 FAQ：「为何将 response 与 exception 设为可选」*。
    
### `Waiter<T>`

泛型 `Waiter` 类使用户可自定义 waiter 配置，并提供自有 `WaiterAcceptor`，用于定义期望状态并控制 waiter 的终止状态。

#### 方法

```java
@SdkPublicApi
public interface Waiter<T> {

    /**
     * It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param pollingFunction the polling function
     * @return the {@link WaiterResponse} containing either a response or an exception that has matched with the
     * waiter success condition
     */
    default WaiterResponse<T> run(Supplier<T> pollingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * It returns when the resource enters into a desired state or
     * it is determined that the resource will never enter into the desired state.
     *
     * @param pollingFunction the polling function
     * @param overrideConfig per request override configuration
     * @return the {@link WaiterResponse} containing either a response or an exception that has matched with the
     * waiter success condition
     */
    default WaiterResponse<T> run(Supplier<T> pollingFunction, WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }
    
    default WaiterResponse<T> run(Supplier<T> pollingFunction, Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return run(pollingFunction, WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }

    /**
     * Creates a newly initialized builder for the waiter object.
     *
     * @param responseClass the response class
     * @param <T> the type of the response
     * @return a Waiter builder
     */
    static <T> Builder<T> builder(Class<? extends T> responseClass) {
        return DefaultWaiter.builder();
    }
}
```
#### 内部类：`Waiter.Builder`

```java
    public interface Builder<T> {

        /**
         * Defines a list of {@link WaiterAcceptor}s to check if an expected state has met after executing an operation.
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> acceptors(List<WaiterAcceptor<T>> waiterAcceptors);

        /**
         * Add a {@link WaiterAcceptor}s
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> addAcceptor(WaiterAcceptor<T> waiterAcceptors);

        /**
        * Defines overrides to the default SDK waiter configuration that should be used
        * for waiters created by this builder.
        *
        * @param overrideConfiguration the override configuration
        * @return a reference to this object so that method calls can be chained together.
        */
        Builder<T> overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);
    }
```
### `AsyncWaiter<T>`

#### 方法
```java
@SdkPublicApi
public interface AsyncWaiter<T> {

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     *  @param overrideConfig per request override configuration
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return runAsync(asyncPollingFunction, WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }
}
```

#### 内部类：`AsyncWaiter.Builder`
#### 方法
```java
    public interface Builder<T> {

        /**
         * Defines a list of {@link WaiterAcceptor}s to check if an expected state has met after executing an operation.
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> acceptors(List<WaiterAcceptor<T>> waiterAcceptors);

        /**
         * Add a {@link WaiterAcceptor}s
         *
         * @param waiterAcceptors the waiter acceptors
         * @return the chained builder
         */
        Builder<T> addAcceptor(WaiterAcceptor<T> waiterAcceptors);

        /**
        * Defines overrides to the default SDK waiter configuration that should be used
        * for waiters created by this builder.
        *
        * @param overrideConfiguration the override configuration
        * @return a reference to this object so that method calls can be chained together.
        */
        Builder<T> overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration);
        
        /**
         * Define the {@link ScheduledExecutorService} used to schedule async attempts
         *
         * @param scheduledExecutorService the schedule executor service
         * @return the chained builder
         */
        Builder<T> scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);
    }
```

#### `WaiterOverrideConfiguration`

`WaiterOverrideConfiguration` 指定 waiter 如何轮询资源。

```java
public final class WaiterOverrideConfiguration {
   //...

    /**
     * @return the optional maximum number of attempts that should be used when polling the resource
     */
    public Optional<Integer> maxAttempts() {
        return Optional.ofNullable(maxAttempts);
    }

    /**
     * @return the optional {@link BackoffStrategy} that should be used when polling the resource
     */
    public Optional<BackoffStrategy> backoffStrategy() {
        return Optional.ofNullable(backoffStrategy);
    }

    /**
     * @return the optional amount of time to wait that should be used when polling the resource
     *
     */
    public Optional<Duration> waitTimeout() {
        return Optional.ofNullable(waitTimeout);
    }
}

```

### `WaiterState`

`WaiterState` 为枚举，定义在满足某条件时 waiter 可迁移到的状态。

```java
public enum WaiterState {
    /**
     * Indicates the waiter succeeded and must no longer continue waiting.
     */
    SUCCESS,

    /**
     * Indicates the waiter failed and must not continue waiting.
     */
    FAILURE,

    /**
     * Indicates that the waiter encountered an expected failure case and should retry if possible.
     */
    RETRY
}
```

### `WaiterAcceptor`

`WaiterAcceptor` 检查操作返回的响应或错误，判断期望条件是否满足，并在匹配时指示 waiter 应迁移到的下一状态。

```java
@SdkPublicApi
public interface WaiterAcceptor<T> {

    /**
     * @return the next {@link WaiterState} that the waiter should be transitioned to if this acceptor matches with the response or error
     */
    WaiterState waiterState();

    /**
     * Check to see if the response matches with the expected state defined by the acceptor
     *
     * @param response the response to inspect
     * @return whether it accepts the response
     */
    default boolean matches(T response) {
        return false;
    }
```

## 常见问题（FAQ）

### 哪些服务会生成 waiter？

若服务存在需要 waiter 支持的操作，则会生成 `{Service}Waiter` 类。

### 为何不在客户端上直接提供 waiter 操作？

选项包括：（1）单独的 waiter 工具类，或（2）在客户端上提供 waiter 操作。

以下对比选项 1 与选项 2，说明为何选择选项 1。

**选项 1：** 单独的 waiter 工具类

```Java
dynamodb.waiter().untilUntilTableExists(describeTableRequest)
```

**选项 2：** 在每个服务客户端上提供 waiter 操作

```Java
dynamodb.waitUntilTableExists(describeTableRequest)
```

**选项 1 优点：**

1. 与现有 S3 utilities、presigner 方法方式一致，例如：`s3Client.utilities()`
2. API 与 v1 waiter 相近，已使用 v1 waiter 的客户迁移到 v2 可能更容易。

**选项 2 优点：**

1. 可发现性略好

**决策：** 采用选项 1，因其与现有功能一致，且选项 2 可能使客户端体积膨胀、使用更困难。

### 为何返回 `WaiterResponse`？

对等待资源创建的 waiter 操作，最后一次成功响应有时包含 resourceId 等重要元数据，客户常需据此对资源执行其他操作。若不返回响应，客户需额外发请求获取。这是来自 v1 waiter 实现的[功能请求](https://github.com/aws/aws-sdk-java/issues/815)。

对将特定异常视为成功状态的 waiter 操作，部分客户仍希望访问该异常以获取 requestId 或原始响应。

因此引入 `WaiterResposne` 包装类，根据触发 waiter 到达期望状态的是响应还是异常提供其中之一；未来如需还可扩展 `attemptExecuted` 等元数据。


### 为何在 `WaiterResponse` 中将 response 与 exception 设为可选？

依据 SDK 风格指南 `UseOfOptional`，

> 当调用方在编译期不明显知道结果是否为 null 时，应使用 `Optional`。

我们在 `WaiterResponse` 中将 `response` 与 `exception` 设为可选，因为二者仅其一存在，且编译期无法确定是哪一个。

以下示例展示如何从 `WaiterResponse` 获取响应。

```java
waiterResponse.matched.response().ifPresent(r -> ...);

```

另一种做法是增加标志字段（如 `isResponseAvailable`）表示响应是否为 null。客户可在访问 `response` 前检查以避免 NPE。

```java
if (waiterResponse.isResponseAvailable()) {
   DescribeTableResponse response = waiterResponse.response();
   ...
}

```

该做法的问题在于：客户浏览 `WaiterResponse` 时可能注意不到 `isResponseAvailable`，仍须自行做空指针检查，否则易遇 NPE；且违背我们对 Optional 使用的指南。

## 参考资料

GitHub 功能请求链接：
- [Waiters](https://github.com/aws/aws-sdk-java-v2/issues/24)
- [Async requests that complete when the operation is complete](https://github.com/aws/aws-sdk-java-v2/issues/286) 

