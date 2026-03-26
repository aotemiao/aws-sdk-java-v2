**设计：** 新功能，**状态：** [开发中](../../../README.md)

# 设计文档（自动请求批处理）

## 引言

* * *
部分客户需要跨多个 AWS 服务进行批量写操作，缺少这些能力要么阻碍其采用 v2 SDK，要么限制 SDK 的可用性。具体而言，v1 曾以 `AmazonSQSBufferedAsyncClient` 形式为 SQS 提供该能力，但同等能力尚未迁移到 v2。

由于许多 AWS 服务已包含批量写操作，通用自动批处理方案不仅可用于 SQS，也可用于任何能从中受益的服务。在 v1 能力之上，batch manager 还可纳入额外简化与抽象，使客户在整个 SDK 中使用批处理更简单。因此 batch manager 期望通过降低成本、提升性能和/或简化实现来使客户受益。

本文说明该通用做法在 Java SDK v2 中的实现方式。


## 设计评审

* * *
决策记录见：https://github.com/aws/aws-sdk-java-v2/blob/master/docs/design/core/batch-utilities/DecisionLog.md

Java SDK 团队决定现阶段实现独立的 batch manager。独立工具与直接在客户端上实现之间仍需进一步讨论。

## 概述

* * *
本文提议的 batch manager 工作方式与 v1 的 `AmazonSQSBufferedAsyncClient` 类似。通过 manager 发出的调用会先缓冲，再以批量请求发往对应服务。v2 还将实现额外能力，例如由 manager 自动对条目数组进行批处理。

客户端缓冲将以通用方式实现，并允许达到各服务允许的最大请求数（例如 SQS 最多 10 条）。从而减少发出的请求次数，降低使用这些 AWS 服务的成本。

## 拟议 API

* * *
v2 SDK 将为可调用批量接口的同步与异步客户端同时提供 batch manager。

### 实例化

**选项 1：从已有客户端实例化**

```
// Sync Batch Manager
SqsClient sqs = SqsClient.create();
SqsBatchManager sqsBatch = sqs.batchManager();

// Async Batch Manager
SqsAsyncClient sqsAsync = SqsAsyncClient.create();
SqsAsyncBatchManager sqsAsyncBatch = sqsAsync.batchManager();
```

**选项 2：从 batch manager 构建器实例化**

```
// Sync Batch Manager
SqsBatchManager sqsBatch = SqsBatchManager.builder()
                                          .client(client)
                                          .overrideConfiguration(newConfig)
                                          .build();

// Async Batch Manager
SqsAsyncBatchManager sqsBatch = SqsAsyncBatchManager.builder()
                                          .client(asyncClient)
                                          .overrideConfiguration(newConfig)
                                          .build();
```

### 一般用法示例：

说明：实习范围聚焦自动批处理与手动 flush。

```
// 1. Automatic Batching
SendMessageRequest request1 = SendMessageRequest.builder()
                                                .messageBody("1")
                                                .build();
SendMessageRequest request2 = SendMessageRequest.builder()
                                                .messageBody("2")
                                                .build();

// Sync
SqsClient sqs = SqsClient.create();
SqsBatchManager sqsBatch = sqs.batchManager();
CompletableFuture<SendMessageResponse> response1 = sqsBatch.sendMessage(request1);
CompletableFuture<SendMessageResponse> response2 = sqsBatch.sendMessage(request2);

// Async
CompletableFuture<SendMessageResponse> response1 = sqsBatch.sendMessage(request1);
CompletableFuture<SendMessageResponse> response2 = sqsBatch.sendMessage(request2);

// 2. Manual Flushing
sqsBatch.flush();
```



### `{Service}BatchManager` 与 `{Service}AsyncBatchManager`

每个可利用批量能力的服务将生成两个类：`{Service}BatchManager` 与 `{Service}AsyncBatchManager`（例如 SQS 的 `SqsBatchManager` 与 `SqsAsyncBatchManager`）。命名约定与 v2 中 `{Service}Client`、`{Service}Manager` 一致。

**同步：**

```
/**
 * Batch Manager class that implements batching features for a sync client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsBatchManager {
 
    /**
     * Buffers outgoing requests on the client and sends them as batch requests to the service. 
     * Requests are batched together according to a batchKey and are sent periodically to the 
     * service as determined by {@link #maxBatchOpenInMs}. If the number of requests for a 
     * batchKey reaches or exceeds {@link #maxBatchItems}, then the requests are immediately 
     * flushed and the timeout on the periodic flush is reset.
     * By default, messages are batched according to a service's maximum size for a batch request. 
     * These settings can be customized via the configuration.
     *
     * @param request the outgoing request.
     * @return a CompletableFuture of the corresponding response.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);
    
    /**
     * Manually flush the buffer for sendMessage requests. Completes when requests
     * are sent. An exception is returned otherwise.
     */
    CompletableFuture<Void> flush();
    
    // Other Batch Manager methods omitted
    // ...
    
    interface Builder {
    
        Builder client (SqsClient client);
        
        /** 
         * Method to override the default Batch Manager configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch Manager configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsBatchManager build();
    
    }
 }
```

**异步：**

```
/**
 * Batch Manager class that implements batching features for an async client.
 */
 @SdkPublicApi
 @Generated("software.amazon.awssdk:codegen")
 public interface SqsAsyncBatchManager {
 
    /**
     * Buffers outgoing requests on the client and sends them as batch requests to the service. 
     * Requests are batched together according to a batchKey and are sent periodically to the 
     * service as determined by {@link #maxBatchOpenInMs}. If the number of requests for a 
     * batchKey reaches or exceeds {@link #maxBatchItems}, then the requests are immediately 
     * flushed and the timeout on the periodic flush is reset.
     * By default, messages are batched according to a service's maximum size for a batch request. 
     * These settings can be customized via the configuration.
     *
     * @param request the outgoing request.
     * @return a CompletableFuture of the corresponding response.
     */
    CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest message);
    
    /**
     * Manually flush the buffer for sendMessage requests. Completes when requests
     * are sent. An exception is returned otherwise.
     */
    CompletableFuture<Void> flush();
    
    // Other Batch Manager methods omitted
    // ...
    
    interface Builder {
    
        Builder client (SqsAsyncClient client);
        
        /** 
         * Method to override the default Batch Manager configuration.
         * 
         * @param overrideConfig The provided overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        Builder overrideConfiguration(BatchOverrideConfiguration overrideConfig);
        
        /** 
         * Convenient method to override the default Batch Manager configuration
         * without needing to create an instance manually.
         * 
         * @param overrideConfig The consumer that provides the
                                 overriding configuration.
         * @return a reference to this object so that method calls can be chained.
         */
        default Builder overrideConfiguration(
                        Consumer<BatchOverrideConfiguration> overrideConfig);
    
        SqsAsyncBatchManager build();
    
    }
 }

```



### `BatchOverrideConfiguration`

```
/**
 * Configuration class to specify how the Batch Manager will implement its
 * batching features. 
 */
public final class BatchOverrideConfiguration {
    
    private final int maxBatchItems;
    
    private final long maxBatchSizeInBytes;
    
    private final Duration maxBatchOpenInMs;
    
    // More fields and methods omitted
    // Focus on including configurable fields from v1
}
```

* * *

## 常见问题

### **哪些服务会生成 Batch Manager？**

已支持批量请求的服务（例如带 `sendMessageBatch` 的 SQS、带 `putRecords` 的 Kinesis）应通过 batch manager 支持，以帮助客户降低成本。

说明：本文聚焦为 SQS 实现 batch manager，以确保 v1 `AmazonSQSBufferedAsyncClient` 能力迁移到 v2，因此代码片段主要围绕 SQS 客户端支持的类型与方法。

### **为何不在底层客户端上直接实现批处理？**

实现批处理曾讨论三种选项：

1. 直接在底层客户端上实现批处理
2. 创建独立高层库
3. 创建独立的 batch manager 类

三种用法示意：

```
SqsAsyncClient sqsAsync = SqsAsyncClient.builder().build();

// Option 1
sqsAsync.automaticSendMessageBatch(message1);
sqsAsync.automaticSendMessageBatch(message1);

// Option 2
 SqsAsyncBatchManager batchManager = SqsAsyncBatchManager
                                        .builder()
                                        .client(sqsAsync)
                                        .build()
batchManager.sendMessage(message1);
batchManager.sendMessage(message2);

// Option 3
SqsAsyncBatchManager batchManager = SqsAsyncBatchManager.batchManager()
batchManager.sendMessage(message1);
batchManager.sendMessage(message2);
```


**选项 1 优点：**

1. 自动批处理略易发现。

**选项 2 优点：**

1. 手写库可比生成工具方法更友好。
2. 与 v1 `AmazonSQSBufferedAsyncClient` 很像，v1→v2 迁移改动小。

**选项 3 优点：**

1. 某服务的批处理相关能力集中在该客户端对应的工具类中。
2. 与 v1 `AmazonSQSBufferedAsyncClient` 很像，迁移改动小。
3. 与 Waiters 等现有工具一致。
4. 易于配置并扩展到多服务。

**决策：** 采用选项 3，因其紧密贴合 v2 风格（尤其类似 waiters 抽象），且在扩展到多服务时仍保持灵活、不致过于复杂。

2021-06-29 的推理见 [decision log](./DecisionLog.md)。

### 为何仅支持一次发送一条消息，而非消息列表或流？

仅提供 `sendMessage` 单条方法，使客户将请求与响应一一对应更简单；若一次接收流或列表（例如 SQS 中 `SendMessageRequest` 对应 `SendMessageResponse` 而非批量包装类），心智负担更大。

多条或流式发送可通过对每条循环调用 `sendMessage` 实现；只要支持 `sendMessage`，日后扩展列表/流也较直接。

### **为何同时支持同步与异步？**

同时支持同步与异步客户端可保持两套 API 不发散，并与 v1 缓冲客户端对齐。实现上只需分别调用同步/异步客户端的底层方法，对客户与 SDK 团队都应直观。


### **为何 `sendMessage` 在同步与异步客户端均返回 CompletableFuture？**

`sendMessage` 会将请求缓冲直至缓冲区满或超时。同步客户端若阻塞至整批 `batchRequest` 完成，可能长达所配置超时。为减少长时间阻塞，同步与异步的 `sendMessage` 均返回 `CompletableFuture`，在底层 `batchRequest` 发出并收到响应后完成。

因此，同步与异步的差异体现在底层所用客户端方法（例如同步 batch manager 在底层使用同步 `sendMessageBatch`，异步则使用异步 `sendMessageBatch`）。


## 参考资料

* * *
各服务的 GitHub 功能请求：

* [SQS](https://github.com/aws/aws-sdk-java-v2/issues/165)
* [Kinesis](https://github.com/aws/aws-sdk-java/issues/1162)
* [Kinesis Firehose](https://github.com/aws/aws-sdk-java/issues/1343)
* [CloudWatch](https://github.com/aws/aws-sdk-java/issues/1109)
* [S3 batch style deletions](https://github.com/aws/aws-sdk-java/issues/1307)

