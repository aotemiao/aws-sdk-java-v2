# AWS SDK for Java v2 开发指南（中文）

本目录为仓库根目录 `docs/guidelines` 的**中文译本**，结构与原文一致。

## 可用指南

### [通用指南](aws-sdk-java-v2-general.md)
适用于所有 AWS SDK Java v2 开发的核心原则、代码风格、设计模式与性能考量。包含命名约定、`Optional` 正确使用、对象方法实现及异常处理模式。

### [异步编程指南](async-programming-guidelines.md)
`CompletableFuture` 使用最佳实践、线程安全考量及异步操作处理。涵盖取消、异常处理与异步代码测试模式。

### [Reactive Streams 指南](reactive-streams-guidelines.md)
实现 Reactive Streams 组件的要求与模式。确保符合 Reactive Streams 规范、正确处理背压，并对 Publisher/Subscriber 实现进行强制性的 TCK 测试。

### [测试指南](testing-guidelines.md)
单元测试、功能测试、集成测试及 Reactive Streams TCK 等专项测试策略。涵盖测试命名、Mock 指导与覆盖率期望。

### [Javadoc 指南](javadoc-guidelines.md)
公共 API 文档标准，包括格式要求、`Javadoc` 标签用法、代码片段规范，以及不同 API 分级（public、protected、internal）的示例。

### [日志指南](logging-guidelines.md)
AWS SDK 专用日志标准：正确使用 SDK Logger、日志级别、结构化日志模式，以及**在已抛出异常时避免重复打 ERROR/WARN** 等关键规则。

### [代码生成指南](code-generation-guidelines.md)
基于 JavaPoet 的代码生成模式与标准，包括 `ClassSpec` 实现、生成任务组织、模型处理，以及用 fixture 校验生成结果的测试方式。

### [命名约定](NamingConventions.md)
类、方法与测试的命名模式，包括服务客户端命名、缩写处理，以及能清楚描述方法、条件与期望行为的测试命名。

### [Optional 使用](UseOfOptional.md)
在 SDK 中何时及如何使用 `Optional`，包括对方法参数、成员变量与返回类型的限制，以保持 API 清晰一致。

### [静态工厂方法](FavorStaticFactoryMethods.md)
优先使用静态工厂方法而非构造函数的惯用法，包括工厂方法命名及对不可变对象与 API 设计的好处。

### [客户端配置](ClientConfiguration.md)
配置对象的结构要求：不可变模式、builder 接口、字段命名，以及集合类型在配置 API 中的处理方式。

### [业务指标（Business Metrics）](business-metrics-guidelines.md)
在 AWS SDK for Java v2 中实现业务指标（User-Agent 遥测短标识）的指南，涵盖以功能为中心的埋点原则、性能考量、功能测试方式及若干示例。
