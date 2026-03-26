**设计：** 新功能，**状态：** [提议中](../../../README.md)

# 事件流重连

事件流允许客户与 AWS 服务在 HTTP/2 连接上进行长时间运行的双向通信。

由于单个请求预期长时间运行，服务通常会提供方式让客户在新 TCP 连接上「恢复」中断的会话。在 Kinesis 的 subscribe-to-shard API 中，每个响应事件包含 `continuationSequenceNumber`，可在请求消息中指定以从断开处继续。在 Transcribe 的流式转写 API 中，每个响应包含语义类似的 `sessionId`。

当前实现要求服务编写高层库来处理该逻辑（例如 Kinesis 消费者库），或要求每位客户手写逻辑。
[此手写代码](CurrentState.java) 冗长且易错。
 
本小型设计概述在网络错误发生时由 SDK 自动重连的 API 选项。

## [API 选项 1：新方法](prototype/Option1.java)

该选项为每个流式操作新增一种方法，客户可用以启用自动重连。客户根据所调用的方法选择是否使用重连。

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create()) {
    // ...
    // 当前方法（行为不变）
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    
    // 新方法：在网络错误时透明重连（名称待定）
    client.startStreamTranscriptionWithReconnects(audioMetadata,
                                                  audioStream,
                                                  responseHandler);
    // ...
}
```

## [API 选项 2：新客户端配置](prototype/Option2.java)

该选项在客户端上新增设置，客户可用以*关闭*自动重连。默认启用自动重连；若用例不需要，需显式关闭。

```Java
// 当前方法更新为在网络错误时透明重连
try (TranscribeStreamingAsyncClient client = 
             TranscribeStreamingAsyncClient.create()) {
    // ...
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    // ...
}

// 新的客户端配置项可用于配置重连行为
try (TranscribeStreamingAsyncClient client = 
             TranscribeStreamingAsyncClient.builder()
                                           .overrideConfiguration(c -> c.reconnectPolicy(ReconnectPolicy.none()))
                                           .build()) {
    // ...
    client.startStreamTranscription(audioMetadata,
                                    audioStream,
                                    responseHandler);
    // ...
}
```

## 对比

| | 选项 1 | 选项 2 |
| --- | --- | --- |
| 可发现性 | - | + |
| 可配置性 | - | + |
| 向后兼容 | + | - |
