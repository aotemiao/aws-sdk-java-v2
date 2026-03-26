# 采集的指标

本文列出 SDK 各组件（核心库与 HTTP 客户端等）采集的指标。

关于 collector 路径：以下路径假定以某次 API 调用为根的 `MetricCollection` 树。

## 核心指标

核心指标集包含 SDK 核心组件采集的全部指标，包括 SDK 服务客户端、请求/响应编组器与解组器、签名器等。

下列指标在代码中的常量定义见 `sdk-core` 内 [`software.amazon.awssdk.core.metrics.CoreMetric`](https://github.com/aws/aws-sdk-java-v2/blob/8c192e3b04892987bf0872f76ba4f65167f3a872/core/sdk-core/src/main/java/software/amazon/awssdk/core/metrics/CoreMetric.java#L24)。

| 名称                          | 类型          | 说明 |
|-------------------------------|---------------|-------------|
| ServiceId                     | `String`      | 服务的唯一 ID。所有 API 调用指标均包含。|
| OperationName                 | `String`      | 正在调用的服务操作名称。所有 API 调用指标均包含。|
| ApiCallDuration               | `Duration`    | API 调用耗时，包含所有重试尝试。|
| ApiCallSuccessful             | `Boolean`     | API 调用是否成功。 |
| BackoffDelayDuration          | `Duration`    | 根据重试策略，本次 API 调用尝试前 SDK 等待的时长。 |
| MarshallingDuration           | `Duration`    | 将 SDK 请求编组为 HTTP 请求所耗时间。|
| CredentialsFetchDuration      | `Duration`    | 为请求获取签名凭证所耗时间。|
| SigningDuration               | `Duration`    | 对 HTTP 请求签名所耗时间。|
| AwsRequestId                  | `String`      | 服务请求的 request ID。|
| AwsExtendedRequestId          | `String`      | 服务请求的扩展 request ID。|
| UnmarshallingDuration         | `Duration`    | 将 HTTP 响应解组为 SDK 响应所耗时间。 |
| ServiceCallDuration           | `Duration`    | 连接服务（或从连接池获取连接）、发送序列化请求并收到初始响应（如 HTTP 状态码与头）所耗时间。**不包含**读取完整响应体的时间。 |
| RetryCount                    | `Integer`    | SDK 执行该请求时的重试次数。0 表示首次即成功、未重试。 |
| ErrorType                     | `String`     | 失败 API 调用尝试所遇错误的大类。<br>可能取值：<br> `Throttling` — 服务返回节流错误。<br>`ServerError` — 服务返回非节流的错误。<br>`ConfiguredTimeout` — 发生客户端超时（API 调用级或单次尝试级）。<br>`IO` — I/O 错误。<br>`Other` — 不属于上述类别的其他错误。|

## HTTP 指标

以下 HTTP 指标由实现 [HTTP SPI](https://github.com/aws/aws-sdk-java-v2/tree/sdk-metrics-development-2/http-client-spi) 的组件采集。具体采集哪些指标取决于实现 SPI 的 HTTP 库；并非所有库都能采集下表每一项。

在 SDK 客户端 API 调用上下文中，所有 `HttpClient` 采集器均为 `ApiCallAttempt` 的子节点；即单次 API 调用尝试的 HTTP 客户端指标完整路径为 `ApiCall` > `ApiCallAttept` > `HttpClient`。

### 通用 HTTP 指标

以下指标对 HTTP/1.1 与 HTTP/2 操作通用。

常量位于 `http-spi` 模块的 `software.amazon.awssdk.http.HttpMetric` 类。

| 名称                          | 类型      | 说明 | 
|-------------------------------|-----------|-------------|
| HttpClientName                | `String`  |  HTTP 客户端名称。 |
| MaxConcurrency                | `Integer` | 对 HTTP/1.1，等于 HTTP 客户端连接池可池化的最大 TCP 连接数。对 HTTP/2，等于 HTTP 客户端可池化的最大流数。|
| LeasedConcurrency             | `Integer` | 当前正由 HTTP 客户端执行的请求数。 |
| PendingConcurrencyAcquires    | `Integer` | 正在等待 HTTP 客户端释放并发槽的请求数。 |
| HttpStatusCode                | `Integer` | HTTP 响应状态码。 |

### HTTP/2 指标

以下指标专属于 HTTP/2 操作。

|  名称                    | 类型      | 说明  |
|--------------------------|-----------|--------------|
| LocalStreamWindowSize    | `Integer` | 本请求所在流的本地 HTTP/2 窗口大小（字节）。 |
| RemoteStreamWindowSize   | `Integer` | 本请求所在流的远端 HTTP/2 窗口大小（字节）。 |
