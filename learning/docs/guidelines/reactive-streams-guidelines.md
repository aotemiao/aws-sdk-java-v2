# Reactive Streams 指南

AWS SDK for Java v2 使用 Reactive Streams 进行异步、非阻塞的数据处理，并支持背压。所有实现必须满足以下要求，以确保功能正确且可互操作。

## 实现指南

### 合规要求

- 所有实现**必须**完全符合 [Reactive Streams 规范](https://github.com/reactive-streams/reactive-streams-jvm)
- 所有实现**必须**通过 [Reactive Streams 技术兼容套件（TCK）](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) 测试
- 对 Reactive Streams 实现的任何代码变更**必须**包含 TCK 验证测试

### 最佳实践
- 开发者**不应**从零开始实现新的 Publisher 或 Subscriber 接口
- 开发者**应**使用现有工具类，例如：
  - `SimplePublisher` — Publisher 接口的简单实现
  - `ByteBufferStoringSubscriber` — 存储所接收 ByteBuffer 的订阅者
  - `SdkPublisher` 中的方法 — 常见 Publisher 操作的工具方法

## 常见模式

- 使用 `SdkPublisher` 实现 SDK 特定的发布者
- 在成功与失败两种场景下都做好资源清理
- 妥善处理取消，包括释放已分配资源
- 所有实现须保证线程安全
- 文档中说明线程安全特性及对执行上下文的假设

## 测试要求

- 所有 Reactive Streams 实现**必须**包含 TCK 验证测试
- 测试**应**覆盖正常运行与边界情况：
  - 流式传输活跃时的取消
  - 错误传播
  - 多种请求场景下的背压处理
  - 所有终止场景下的资源清理

## 参考资料

- [Reactive Streams 规范](https://github.com/reactive-streams/reactive-streams-jvm)
- [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck)
