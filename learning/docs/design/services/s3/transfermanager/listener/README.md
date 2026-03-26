**设计：** 新功能，
**父功能：** [TransferManager](..)，
**状态：** [开发中](..)

# v2 Transfer Manager 进度监听器

## 概述

Java SDK v1 为 `TransferManager` 发起的上传/下载提供 `ProgressListener` 概念。典型用例是让用户显示进度条以跟踪长时间传输。Java SDK v2 目前缺少同等能力。我们希望在提供该能力的同时，重新审视如何最好地暴露这类行为。

## Java SDK v1 背景

v1 `ProgressListener` 接口可概括为：

```
public interface ProgressListener {
    void progressChanged(ProgressEvent progressEvent);
}
```

```
@Value
public class ProgressEvent {
    ProgressEventType eventType;
    long bytes;
    long bytesTransferred;
}
```

```
public enum ProgressEventType {
    REQUEST_CONTENT_LENGTH_EVENT,
    RESPONSE_CONTENT_LENGTH_EVENT,
    REQUEST_BYTE_TRANSFER_EVENT,
    RESPONSE_BYTE_TRANSFER_EVENT,
    RESPONSE_BYTE_DISCARD_EVENT,
    CLIENT_REQUEST_STARTED_EVENT,
    HTTP_REQUEST_STARTED_EVENT,
    HTTP_REQUEST_COMPLETED_EVENT,
    HTTP_REQUEST_CONTENT_RESET_EVENT,
    CLIENT_REQUEST_RETRY_EVENT,
    HTTP_RESPONSE_STARTED_EVENT,
    HTTP_RESPONSE_COMPLETED_EVENT,
    HTTP_RESPONSE_CONTENT_RESET_EVENT,
    CLIENT_REQUEST_SUCCESS_EVENT,
    CLIENT_REQUEST_FAILED_EVENT,
    // TM-specific events:
    TRANSFER_PREPARING_EVENT,
    TRANSFER_STARTED_EVENT,
    TRANSFER_COMPLETED_EVENT,
    TRANSFER_FAILED_EVENT,
    TRANSFER_CANCELED_EVENT,
    TRANSFER_PART_STARTED_EVENT,
    TRANSFER_PART_COMPLETED_EVENT,
    TRANSFER_PART_FAILED_EVENT;
}
```

**观察：**

1. 可触发 progress listener 更新的事件类型*很多*。
2. 部分事件通用，部分仅见于与 `TransferManager` 相关的请求。
3. 向用户暴露的字段在各事件类型间不变，始终只有 `bytes` 与 `bytesTransferred`。
    1. 这两个字段的含义取决于关联的事件类型。
    2. 例如 `{REQUEST_CONTENT_LENGTH_EVENT, bytes}` 表示 content-length（预计发送的字节数）。
    3. 而 `{REQUEST_BYTE_TRANSFER_EVENT, bytes}` 表示刚写入的字节数。
    4. 其他组合在不适用时返回 `0`。
    5. 无法用简单 `long` 表达的信息无法传递。
4. `ProgressListener` 不暴露历史或事件累积。例如用户需自行保存 content length，并持续累加已写字节数。

* * *
*注：[v1 公开文档](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-transfermanager.html#transfermanager-get-status-and-progress) 目前写道，可用 ProgressListener 如下计算总体进度：*

```
@Override
public void progressChanged(ProgressEvent e) {
    double pct = e.getBytesTransferred() * 100.0 / e.getBytes();
    eraseProgressBar();
    printProgressBar(pct);
}
```

*这是错误的。单个 ProgressEvent 既不记录曾见过的 content length，也不累计至今已处理的字节总数。这说明这些参数多重、过载的含义会导致困惑的用户体验。*
* * *
与 v1 `ProgressListener` 并列的是 v1 `TransferProgress` 类。该类有状态，*会*跟踪至今已处理的字节总数。每个 `TransferManager` 发起的传输都关联一个 `TransferProgress`，可如下查询：

```
TransferProgress progress = transfer.getProgress();
progress.getBytesTransferred();
progress.getTotalBytesToTransfer();
progress.getPercentTransferred();
```

在内部，`TransferManager` 用自有 `ProgressListener` 实现记录与其配对的 `TransferProgress` 的更新。理想且正确的事件驱动用户实现可能是在 `ProgressListener` 内查询对应 `TransferProgress`。（遗憾的是 `ProgressListener` 不直接暴露 `Transfer` 或 `TransferProgress` 引用，在传输开始前声明较为麻烦。）

## Java SDK v2 背景

v2 目前虽无 `ProgressListener` 概念，但大量使用 `ExecutionInterceptor`，提供部分相似能力。`ExecutionInterceptor` 实现[拦截过滤器模式](https://en.wikipedia.org/wiki/Intercepting_filter_pattern)，可组合地响应不同事件并施加转换。关键差异在于 `ExecutionInterceptor` 多面向*可改变*请求/响应的操作，方法名也强化这一点，例如 `modifyRequest`、`modifyHttpContent` 等。

v2 `ExecutionInterceptor` 可概括为：

```
public interface ExecutionInterceptor {
    // context = SdkRequest
    default void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {}
    default SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {return context.request();}
    default void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {}
    // context += SdkHttpRequest, RequestBody (contentLength)
    default void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {}
    default SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.httpRequest();}
    default Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.requestBody();}
    default Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {return context.asyncRequestBody();}
    default void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {}
    // context += SdkHttpResponse, responseBody
    default void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {}
    default SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.httpResponse();}
    default Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.responsePublisher();}
    default Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context, ExecutionAttributes executionAttributes) {return context.responseBody();}
    default void beforeUnmarshalling(Context.BeforeUnmarshalling context, ExecutionAttributes executionAttributes) {}
    // context += SdkResponse
    default void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {}
    default SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {return context.response();}
    default void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {}
    
    // Separate (optional) context attributes
    default Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {return context.exception();}
    default void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {}
}
```

**观察：**

1. 各 context 对象*继承*前一阶段 context，随请求生命周期逐步增加可用参数。
2. 相较 v1 单方法+多枚举，v2 使用多方法，从而更强地将事件类型与可用参数绑定（而非用单个 `long` 承载多重含义）。
3. v1 可能对枚举做 *switch*，v2 则选择性重写方法；有时更清晰，有时导致重复代码或遗漏事件。
4. 我们能转换 body 与输入/输出流，但除包装为计量流外，缺少易用的 body 进度测量手段。
5. 如前所述，该接口明确面向可变场景，对仅想「旁听」事件的客户可能显得过重。

## v2 进度监听器目标

1. 提供直观接口，使用户能跟踪 `TranserManager`（原文 TranserManager 为笔误）发起的上传进度
2. 便于实现上传/下载进度跟踪中最常见的逻辑（如进度条）
3. **复用**：设计虽以 TM 为主，仍应寻找使其他场景受益的方式
4. **可扩展**：满足 TM 需求时不必在通用场景一次做到完美；未来应能轻松、安全地扩展（向后兼容）
5. 注意与 `ExecutionInterceptor` 的可能重叠；尽量避免重复劳动，并明确有意重叠之处
6. 性能影响应极小，且不影响未使用新能力的现有用法

## 提案

1. 在 `sdk-core` 中新增 `ExecutionListener` 接口（名称待定）：
    1. 最初为 `TranserManager` 设计，同时可形成新的用户友好公开 API。
    2. `ExecutionListener` 能力与 `ExecutionInterceptor` 接近，但**仅**用于非修改、不改变控制流的操作。
    3. 所有方法返回 `void`；抛出的异常被抑制（并打警告日志），强调 listener 不用于控制流。
    4. `Listener` 可在今日 `Interceptor` 可声明的任意位置声明（客户端级、请求级）。
    5. 多数场景下 `Listener` 方法*少于* `Interceptor`，例如不必同时暴露 `before` 与 `after`，只暴露类似 `after` 的回调即可。
    6. 某些场景下 `Listener` 也可能*多于* `Interceptor`，因请求/响应生命周期中有些有趣事件未必可修改，故与 `Interceptor` 无关。
    7. 与 `Interceptor` 类似，将 context 接口按事件链式串联；必要时在 context 中保存关键事件历史（如至今已写/读字节数），减少用户自行维护状态。
    8. `Listener` 作为向客户推荐的更细粒度日志、自定义指标等插桩方式。
    9. 可选优化：`Listener` 可声明为 *blocking* 或 *non-blocking*。阻塞 listener 在独立线程池执行（类似 future 完成回调），非阻塞则内联执行。默认假定均为阻塞。
2. 新增专属于 `TransferManager` 的 `TransferListener` 接口。
    1. 提供传输生命周期相关的回调。
    2. 与 v1 不同，`TransferListener` 与 `ExecutionListener` 明确分离，不混为一谈。
        1. 二者不会 1:1 对齐，因单次传输可能包含多个请求（多段上传或多段 Range GET）。
    3. `TransferListener` 可在创建 `TransferManager` 或单个 `TransferRequest` 时声明。
    4. `TransferListener` *不能*在 `PutObjectRequest` 或 `GetObjectRequest` 上声明（这些仅接受 `ExecutionListener`）。
3. 将 v1 `TransferProgress` 概念扩展为相当的 v2 `TransferProgress`。
    1. 与 v1 不同，`TransferProgress` 可从 `TransferListener` 回调直接访问。
    2. 与 v1 不同，`TransferProgress` 不可变。
    3. 与 v1 相同，也可从现有 `Transfer`（`Upload`/`Download`）直接访问。
    4. 与 v1 相同，支持轮询式查询传输状态。
    5. 与 v1 不同，在单次 listener 更新中提供便捷的（不可变）视图。
    6. 相对 v1 改进：`TransferProgress` 一等支持查询百分比、已用时间、平均速率等，显著改善主要用例的开发体验。

## 拟议公开接口（草案）

```
public interface ExecutionModeAware {
     enum ExecutionMode {
        INLINE, // executed in EventLoop thread, use w/ caution, non-blocking only
        OFFLOAD // executed in separate thread pool, slower but safer, blocking okay
    }
    
    default ExecutionMode executionMode() {
        return ExecutionMode.OFFLOAD;
    }
}
```

```
// Generic, request-level listener
public interface ExecutionListener extends ExecutionModeAware {
    // context = SdkRequest
    // (called 1x for a "logical" request)
    default void requestCalled(Context.RequestSent context) {}
    
    // context += SdkHttpRequest, RequestBody (contentLength)
    // (called 1x for a "logical" request)
    default void requestPrepared(Context.RequestSent context) {}
    
    // (called for each "physical" request (incl. retry-attempt))
    default void requestSent(Context.RequestSent context) {}
    
    // context += requestBytesSent
    default void requestBytesSent(Context.RequestBytesSent context) {}
    
    // context += SdkResponse, SdkHttpResponse, responseBody
    default void responseReceived(Context.ResponseReceived context) {}
    
    // context += responseBytesReceived
    default void responseBytesReceived(Context.ResponseBytesReceived context) {}
    
    // Separate (optional) context attributes
    default void executionFailure(Context.ExecutionFailure context) {}
    
    // TODO: Consider other callbacks for more advanced retry notifications
    // TODO: Consider more low-level callbacks, see v1 events for reference
}
```

```
// TM-specific listener
public interface TransferListener extends ExecutionModeAware {
    // context = TransferRequest (UploadRequest/DownloadRequest), TransferProgress
    default void transferInitiated(Context.TransferInitiated context) {}  
      
    default void bytesTransferred(Context.BytesTransferred context) {}
    
    // context += CompletedTransfer (CompletedUpload/CompletedDownload)
    default void transferComplete(Context.TransferComplete context) {}

    // Separate (optional) attributes
    default void executionFailure(Context.ExecutionFailure context) {}
    
    // TODO: Consider other callbacks for more advanced retry notifications
    
    // We may wish to warn that num bytes transferred may temporarily
    // decrease in the event of a retry.
}
```

```
public interface TransferProgress {
     // Attributes
    Instant startTime();
    long totalBytesTransferred();
    OptionalLong totalTransferSize();
    
    // Convenience methods (optional methods depend on a known transferSize)
    Duration elapsedTime();
    double averageBytesPer(TimeUnit timeUnit);
    OptionalDouble percentageTransferred();
    OptionalLong bytesRemaining();
    Optional<Duration> estimatedTimeRemaining(); // uses avg, users may want EWMA
}
```

此外，扩展现有 `Transfer` 接口，加入 `TransferProgress` 属性：

```
  public interface Transfer {
      CompletableFuture<? extends CompletedTransfer> completionFuture();
+     TransferProgress progress();
  }
```

## 示例

此前用 `ProgressListener` 计算进度百分比（不正确）的文档示例，现可用 `TransferListener` 直接实现：

```
@Override
public void bytesTransferred(Context.BytesTransferred context) {
     context.transferProgress().percentageTransferred().ifPresent(this::updateProgressBar);
}
```

## 扩展与向后兼容

向 `ExecutionListener` 增加新事件是向后兼容的：

* 对链式 context `A → B → C`，增加 `A → B → B′ → C` 仍安全，因为 `C` 仍继承 `B`。
* 且所有事件方法设计为返回 `void`，可安全增加默认空实现。

重排事件、在 context 接口间迁移参数、或将一个事件拆成多个，均*不*向后兼容。需格外谨慎，首次就确定合适粒度。

## 实现

`ExecutionListener` 与 `ExecutionInterceptor` 高度相似，实现方式也类似：代码库中几乎所有接受或调用 `Interceptor` 之处都扩展为同时支持 `Listener`。实现上 `Listener` 可与 `Interceptor` 并行，或在 `Listener` 为 `Interceptor` 子集时直接实现为 `Interceptor`。

`TransferManager` 为每个子请求声明自有 `ExecutionListener` 实现，以调用已注册的 `TransferListener`：即在任意 `ExecutionListener` 检测到字节进度时，创建新的不可变 `TransferProgress` 并调用所有 `TransferListener`。内部 `ExecutionListener` 可安全设为 `ExecutionMode.INLINE`，因实现完全可控；但仍须尊重各 listener 自身的 `ExecutionMode`。

两种实现均可考虑优化：在创建新 `TransferProgress` 或 context 之前先确认 listener 列表非空，以在未配置 listener 时减少分配。

## 杂项评论/问题

Java v1 TM 还支持多文件传输的 [MultipleFileUpload](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/MultipleFileUpload.html) 与 Download 接口，可获取子传输引用。`MultipleFileUpload` 也继承 `Transfer`，因而有自有 `ProgressListener`。v2 应实现相同的子传输与 listener 逻辑。

Java v1 TM 支持向进行中的传输*动态添加* listener，与 Java `CompletableFuture` 等 API 一致，对部分用户更友好。需决定 v2 是否支持，或是否更偏向不可变设计。理论上可支持动态添加 `TransferListener` 而不要求动态添加 `ExecutionListener`。

是否应将 `ExecutionListener` 拆成更细步骤（如 `requestExecuted`、`requestMarshalled`、`requestSent`）？这些步骤之间无网络延迟，单独暴露是否价值不明；主要好处是某步出错时可观察性更好。

`ExecutionListener` 的替代命名？`ProgressListener`、`CallListener`、`RequestListener` 等。

