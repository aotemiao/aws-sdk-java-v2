# AWS SDK v2 异步编程指南（中文）

## CompletableFuture 指南

### CompletableFuture 最佳实践

- **阅读文档**：务必阅读 [CompletionStage Javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)，理解 `CompletableFuture` 的细微信号语义
- **获取结果时优先非阻塞**：

```java
    // 尽量避免 — 会阻塞当前线程
    String result = future.get();

    // 更好 — 使用回调
    future.thenAccept(result -> processResult(result));
```
- **为异常补充堆栈**：使用 `CompletableFuture#join` 时，用 `CompletableFutureUtils#joinLikeSync` 保留完整堆栈
- **勿忽略结果**：若父阶段可能失败，请勿忽略新生成 `CompletionStage` 的结果（除非你完全确定安全）
- **勿假设线程**：永远不要假设由哪个线程完成 future
  - `CompletableFuture` 回调可能在以下线程执行：
    - 完成该 future 的线程
    - 公共 `ForkJoinPool` 中的线程（对未指定执行器的 async 变体）
    - 所提供 executor 中的线程
  - 行为可能因以下因素变化：
    - 添加回调时 future 是否已完成
    - 使用 async / 非 async 方法（例如 `thenApply` 与 `thenApplyAsync`）
    - 具体 JVM 实现与平台
  - 在以下场景务必保证线程安全：
    - 在回调中访问共享状态
    - 修改 UI 组件（使用合适的 UI 线程）
    - 使用 `ThreadLocal` 等带线程亲和性的资源
  - 错误假设示例：
  ```java
  // 错误：假设回调在特定线程运行
  ThreadLocal<Context> contextHolder = new ThreadLocal<>();
  
  public void processAsync(CompletableFuture<Response> responseFuture) {
      Context context = new Context();
      contextHolder.set(context);  // 在当前线程设置
      
      responseFuture.thenApply(response -> {
          // 错误：假设 contextHolder 仍有 context
          Context ctx = contextHolder.get();  // 若在另一线程运行可能为 null！
          return processWithContext(response, ctx);
      });
  }
  ```
  - 正确做法：
  ```java
  // 正确：显式将 context 传入回调
  public void processAsync(CompletableFuture<Response> responseFuture) {
      Context context = new Context();
      
      responseFuture.thenApply(response -> {
          return processWithContext(response, context);
      });
  }
  ```
- **始终提供自定义执行器**：不要在不提供自定义执行器的情况下使用 `CompletableFuture#xxAsync`（如 `runAsync`、`thenComposeAsync`），默认 `ForkJoinPool.commonPool()` 行为因平台而异
- **正确处理取消**：`CompletableFuture` 不会自动传播取消，请用 `CompletableFutureUtils#forwardExceptionTo` 手动传播
- **避免链式串联多个 API 调用**：若无适当处理，容易引发取消方面的问题
- **避免阻塞**：永远不要在链中使用 `get()` 或 `join()`，这会抵消异步意义
- **正确处理异常**：务必通过 `exceptionally()` 或 `handle()` 处理异常
```java
   CompletableFuture.supplyAsync(() -> fetchData())
       .exceptionally(ex -> {
           logger.error("Error processing data", ex);
           return fallbackData();
       }, executor);
```
- **选择合适的完成方法**：
  - `whenComplete()`：无需转换结果，但需要同时看到结果与异常以做清理或副作用
  - `handle()`：需要将成功与异常都映射为新结果
  - `exceptionally()`：仅需处理异常情况
  - `thenApply()`：转换结果
  - `thenAccept()`：消费结果，无返回值
  - `thenRun()`：不关心结果，仅执行动作
  - `thenCompose()`：下一步仍返回 `CompletableFuture`
- **充分测试异步代码**：
  - 测试中可用 `CompletableFuture.join()` 等待完成
  - 为测试设置合理超时

## 相关指南

Reactive Streams 实现请参阅 [Reactive Streams 指南](reactive-streams-guidelines.md)。

## 参考

- [CompletableFuture JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
- [CompletionStage JavaDoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
