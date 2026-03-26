## 设计文档

### 新功能

**开发中**

*以下功能已开始开发。完成时间无法承诺，最终形态也可能与文档中的提案有较大差异。*

* [S3 Transfer Manager](services/s3/transfermanager/README.md) —
  简化向 Amazon S3 上传与下载对象。

**提案中**

*以下功能处于提案阶段。开发启动后将转入「开发中」，若决定不做则标记为「已拒绝」。何时敲定无法承诺。*

* [事件流替代语法](core/event-streaming/alternate-syntax/README.md)
  — 为非资深用户简化与事件流式服务的交互。
* [事件流自动重连](core/event-streaming/reconnect/README.md)
  — 在网络错误打断事件流会话时自动重连。
* [Tagged Unions](core/tagged-unions/README.md)
  — 改进联合类型（如 DynamoDB 的 `AttributeValue`）的易用性。

**已发布**

*这些功能可视为「基本实现完成」。新功能开发永无绝对终点；此处认为「足够可用」，其余设计点或能力可按需求增量交付。*

* [Request Presigners](core/presigners/README.md) — 支持为稍后执行的请求生成签名。
* [DynamoDB Enhanced Client](services/dynamodb/high-level-library/README.md)
  — 简化向 Amazon DynamoDB 读写对象。

**已拒绝**

无

### 约定

**提案中**

无

**已采纳**

* [类初始化](../guidelines/FavorStaticFactoryMethods.md) — 初始化类的约定。
* [命名约定](../guidelines/NamingConventions.md) — 类命名约定。
* [客户端配置](../guidelines/ClientConfiguration.md) — 客户端配置对象的约定。
* [Optional 使用](../guidelines/UseOfOptional.md) — 使用
  [java.util.Optional](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) 的约定。
* [异步编程 / CompletableFuture](../guidelines/async-programming-guidelines.md) — 使用
  [java.util.concurrent.CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) 等的约定。

**已拒绝**

无
