**设计：** 新功能，**状态：** [提议中](../../../README.md)

# 事件流替代语法

事件流允许客户与 AWS 服务在 HTTP/2 连接上进行长时间运行的双向通信。

当前事件流 API 语法对高级用户足够，但有若干缺点：

1. 即便相对简单的场景也必须使用 Reactive Streams API。Reactive Streams 功能强大，但若无外部文档与库辅助则难以使用。
2. 所有响应处理必须在回调（`ResponseHandler` 抽象）中完成，向应用其余部分传递信息较困难。

本小型提案建议为所有事件流操作提供一种客户可选的替代语法。

## 提案

将为每个事件流操作新增方法：`Running{OPERATION} {OPERATION}({OPERATION}Request)`（及其 consumer-builder 变体）。

将为每个事件流操作新增类型：`Running{OPERATION}`：

```Java
interface Running{OPERATION} extends AutoCloseable {
    // A future that is completed when the entire operation completes.
    CompletableFuture<Void> completionFuture();

    /**
     * Methods enabling reading individual events asynchronously, as they are received.
     */
    
    CompletableFuture<Void> readAll(Consumer<{RESPONSE_EVENT_TYPE}> reader);
    CompletableFuture<Void> readAll({RESPONSE_EVENT_TYPE}Visitor responseVisitor);
    <T extends {RESPONSE_EVENT_TYPE}> CompletableFuture<Void> readAll(Class<T> type, Consumer<T> reader);

    CompletableFuture<Optional<{REQUEST_EVENT_TYPE}>> readNext();
    <T extends {RESPONSE_EVENT_TYPE}> CompletableFuture<Optional<T>> readNext(Class<T> type);

    /**
     * Methods enabling writing individual events asynchronously.
     */

    CompletableFuture<Void> writeAll(Publisher<? extends {REQUEST_EVENT_TYPE}> events);
    CompletableFuture<Void> writeAll(Iterable<? extends {REQUEST_EVENT_TYPE}> events);
    CompletableFuture<Void> write({REQUEST_EVENT_TYPE} event);

    /**
     * Reactive-streams methods for reading events and response messages, as they are received.
     */
    Publisher<{RESPONSE_EVENT_TYPE}> responseEventPublisher();
    Publisher<{OPERATION}Response> responsePublisher();

    /**
     * Java-8-streams methods for reading events and response messages, as they are received.
     */
     
    Stream<{RESPONSE_EVENT_TYPE}> blockingResponseEventStream();
    Stream<{OPERATION}Response> blockingResponseStream();

    @Override
    default void close() {
        completionFuture().cancel(false);
    }
}
```

该类型使客户可按 Reactive Streams 或 Java 8 流式用法使用操作，取决于其如何管理线程与背压。

值得注意的是，`Running{OPERATION}` 上每个方法仍为非阻塞，且不会直接抛出异常。任何返回类型自身包含阻塞方法的方法均以 `blocking` 为前缀，例如 `Stream<{RESPONSE_EVENT_TYPE}> 
blockingResponseEventStream()`。

**示例 1：Transcribe 的 `startStreamTranscription` 与 Reactive Streams**

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create();
     // Create the connection to transcribe and send the initial request message
     RunningStartStreamTranscription transcription =
            client.startStreamTranscription(r -> r.languageCode(LanguageCode.EN_US)
                                                  .mediaEncoding(MediaEncoding.PCM)
                                                  .mediaSampleRateHertz(16_000))) {

    // Use RxJava to create the audio stream to be transcribed
    Publisher<AudioStream> audioPublisher =
            Bytes.from(audioFile)
                 .map(SdkBytes::fromByteArray)
                 .map(bytes -> AudioEvent.builder().audioChunk(bytes).build())
                 .cast(AudioStream.class);

    // Begin sending the audio data to transcribe, asynchronously
    transcription.writeAll(audioPublisher);

    // Get a publisher for the transcription
    Publisher<TranscriptResultStream> transcriptionPublisher = transcription.responseEventPublisher();

    // Use RxJava to log the transcription
    Flowable.fromPublisher(transcriptionPublisher)
            .filter(e -> e instanceof TranscriptEvent)
            .cast(TranscriptEvent.class)
            .forEach(e -> System.out.println(e.transcript().results()));

    // Wait for the operation to complete
    transcription.completionFuture().join();
}
```

**示例 2：Transcribe 的 `startStreamTranscription` 不使用 Reactive Streams**

```Java
try (TranscribeStreamingAsyncClient client = TranscribeStreamingAsyncClient.create();
    // Create the connection to transcribe and send the initial request message
    RunningStartStreamTranscription transcription =
            client.startStreamTranscription(r -> r.languageCode(LanguageCode.EN_US)
                                                  .mediaEncoding(MediaEncoding.PCM)
                                                  .mediaSampleRateHertz(16_000))) {
    
    // Asynchronously log response transcription events, as we receive them
    transcription.readAll(TranscriptEvent.class, e -> System.out.println(e.transcript().results()));

    // Read from our audio file, 4 KB at a time
    try (InputStream reader = Files.newInputStream(audioFile)) {
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = reader.read(buffer)) != -1) {
            if (bytesRead > 0) {
                // Write the 4 KB we read to transcribe, and wait for the write to complete
                SdkBytes audioChunk = SdkBytes.fromByteBuffer(ByteBuffer.wrap(buffer, 0, bytesRead));
                CompletableFuture<Void> writeCompleteFuture =
                        transcription.write(AudioEvent.builder().audioChunk(audioChunk).build());
                writeCompleteFuture.join();
            }
        }
    }

    // Wait for the operation to complete
    transcription.completionFuture().join();
}
```

**示例 3：Kinesis 的 `subscribeToShard` 与 Java 8 Stream**

```Java
try (KinesisAsyncClient client = KinesisAsyncClient.create();
     // Create the connection to Kinesis and send the initial request message
     RunningSubscribeToShard transcription = client.subscribeToShard(r -> r.shardId("myShardId"))) {

    // Block this thread to log 5 Kinesis SubscribeToShardEvent messages
    transcription.blockingResponseEventStream()
                 .filter(SubscribeToShardEvent.class::isInstance)
                 .map(SubscribeToShardEvent.class::cast)
                 .limit(5)
                 .forEach(event -> System.out.println(event.records()));
}
```
