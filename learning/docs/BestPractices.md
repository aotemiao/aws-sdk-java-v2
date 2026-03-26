## AWS Java SDK 2.x 最佳实践

以下是使用 AWS Java SDK 2.x 的推荐做法。

### 尽量复用 SDK 客户端

每个 SDK 客户端维护自己的 HTTP 连接池，以便新请求复用连接、减少建连时间。建议共享同一客户端实例，避免因连接池过多却未充分利用而带来开销。所有 SDK 客户端均为线程安全。

若不宜共享客户端实例，在不再需要时请调用 `client.close()` 释放资源。

### 关闭客户端操作返回的输入流

对于流式操作（如 `S3Client#getObject`），若直接使用 `ResponseInputStream`，建议：

- 尽快读完流中的全部数据
- 尽快关闭输入流

原因是该输入流直接来自 HTTP 连接；只有数据读完且流关闭后，底层连接才能复用。若不遵守，客户端可能因大量空闲未关闭的 HTTP 连接而耗尽资源。

### 基于 `SdkHttpClient` 的自定义客户端：关闭 `responseBody()` 的输入流

在实现自定义 `SdkHttpClient` 时：

- 关闭 [responseBody](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/http/HttpExecuteResponse.html#responseBody()) 输入流：务必关闭 `responseBody` 输入流以释放 HTTP 连接，即使处理错误响应时也应如此。
- SDK 要求：必须通过关闭输入流来有效管理 HTTP 连接。
- 错误响应：即使错误响应，SDK 仍可能创建用于读取错误信息的输入流。
- 一致做法：成功与失败路径下，都应确保关闭 `responseBody` 中的输入流。

### 根据性能测试调优 HTTP 配置

SDK 提供一套适用于常见场景的[默认 HTTP 配置][default http configurations]。建议结合自身应用场景，通过性能测试调优。

### Netty 异步客户端使用 OpenSSL

默认情况下，`NettyNioAsyncHttpClient` 使用 JDK 作为 `SslProvider`。在本地测试中，使用 `OpenSSL` 通常比 JDK 性能更好；Netty 社区也推荐 OpenSSL，参见 [Netty TLS with OpenSSL]。需在依赖中加入 `netty-tcnative`，参见 [Configuring netty-tcnative]。

正确配置 `netty-tcnative` 后，`NettyAsyncHttpClient` 会自动选用 OpenSSL，也可在构建器中显式指定：

```
   NettyNioAsyncHttpClient.builder()
                          .sslProvider(SslProvider.OPENSSL)
                          .build();
```

### 善用超时配置

SDK 开箱提供请求超时配置，可通过 `ClientOverrideConfiguration#apiCallAttemptTimeout` 与 `ClientOverrideConfiguration#ApiCallTimeout` 设置：

```java
  S3Client.builder()
          .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(API_CALL_TIMEOUT))
                                       .apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
          .build();
```

- `ApiCallAttemptTimeout`：单次 HTTP 尝试的最长时间；若单次尝试超时，仍可重试。
- `ApiCallTimeout`：包含所有重试在内的整次调用的最长时间。

默认关闭超时。二者配合可为「总耗时」和「单次请求」设置硬上限，在慢请求上更快失败。

[default http configurations]: https://github.com/aws/aws-sdk-java-v2/blob/master/http-client-spi/src/main/java/software/amazon/awssdk/http/SdkHttpConfigurationOption.java
[Netty TLS with OpenSSL]: https://netty.io/wiki/requirements-for-4.x.html#tls-with-openssl
[Configuring netty-tcnative]: https://netty.io/wiki/forked-tomcat-native.html
